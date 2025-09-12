import asyncio
import json
import logging
import requests
import websockets
import pymysql

# ===================== 계정 정보 =====================
APP_KEY = "PSgXaH8s5e6LjYFuJcx6XIEt7fk4idpDKiOU"
SECRET_KEY = "rI+GejeP/8lysdn0ooKkuSNLTlXERdY6UtJHLNjaHwwRtd+cQ+70RKZyoDiU9SHjWMchZn0odqt+bitxZ47EnkSq3LPy/EvOrwZPmHJxCTfdKdlqPNy5oS6OE22xsS99whjbjuU4zGOL0AnwWRUo="
CUSTTYPE = "P"
TR_TYPE = "1"
SUBSCRIBE_CODES = ["005930", "035720", "000880"]

DB_CONFIG = {
    "host": "localhost",
    "user": "root",
    "password": "ssafy",
    "database": "kospi200",
    "charset": "utf8mb4"
}

latest_data = {}  # ticker별 최신 데이터
prev_data = {}    # 1분 전 가격 저장

ticker_name_map = {
    "005930": "삼성전자",
    "035720": "카카오",
    "000880": "한화솔루션"
}

# ===================== 승인 토큰 =====================
def get_approval_key():
    url = "https://openapivts.koreainvestment.com:29443/oauth2/Approval"
    body = {"grant_type": "client_credentials", "appkey": APP_KEY, "secretkey": SECRET_KEY}
    response = requests.post(url, json=body)
    if response.status_code == 200:
        logging.info("✅ 승인 토큰 발급 성공")
        return response.json().get("approval_key")
    else:
        logging.error(f"❌ 토큰 발급 실패: {response.status_code} {response.text}")
        return None

# ===================== DB =====================
def create_table_if_not_exists():
    conn = pymysql.connect(**DB_CONFIG)
    cursor = conn.cursor()
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS realtime_stock (
            ticker VARCHAR(10) PRIMARY KEY,
            company_name VARCHAR(255),
            price DOUBLE,
            rate DOUBLE
        )
    """)
    conn.commit()
    cursor.close()
    conn.close()

def save_to_db():
    conn = pymysql.connect(**DB_CONFIG)
    cursor = conn.cursor()
    for code, info in latest_data.items():
        curr_price = float(info["price"].replace(",", ""))
        prev_price = prev_data.get(code)
        rate = ((curr_price - prev_price) / prev_price * 100) if prev_price else 0
        company_name = ticker_name_map.get(code, code)
        cursor.execute("""
            INSERT INTO realtime_stock (ticker, company_name, price, rate)
            VALUES (%s, %s, %s, %s)
            ON DUPLICATE KEY UPDATE
                company_name=VALUES(company_name),
                price=VALUES(price),
                rate=VALUES(rate)
        """, (code, company_name, curr_price, rate))
    conn.commit()
    cursor.close()
    conn.close()

# ===================== 로그 출력 =====================
async def print_log_every_minute():
    global prev_data
    while True:
        await asyncio.sleep(60)
        logging.info("📊 1분 단위 요약:")
        for code, info in latest_data.items():
            prev_price = prev_data.get(code)
            curr_price = float(info["price"].replace(",", ""))
            rate = ((curr_price - prev_price) / prev_price * 100) if prev_price else 0
            logging.info(f"{code} ({info['company_name']}): {curr_price}원, 변동률: {rate:.2f}%")
        save_to_db()
        prev_data = {code: float(info["price"].replace(",", "")) for code, info in latest_data.items()}

# ===================== 한국투자 API 웹소켓 =====================
async def start_koreainvest_ws(approval_key):
    url = "ws://ops.koreainvestment.com:31000"
    async with websockets.connect(url, ping_interval=20) as ws:
        logging.info("✅ 한국투자 웹소켓 연결 성공")
        header = {
            "approval_key": approval_key,
            "custtype": CUSTTYPE,
            "tr_type": TR_TYPE,
            "content-type": "utf-8"
        }
        for code in SUBSCRIBE_CODES:
            body = {"input": {"tr_id": "H0STCNT0", "tr_key": code}}
            await ws.send(json.dumps({"header": header, "body": body}))
            logging.info(f"📡 구독 요청 전송: {code}")
        asyncio.create_task(print_log_every_minute())
        while True:
            message = await ws.recv()
            if "PINGPONG" in message or message.startswith("{"):
                continue
            parts = message.split("|")
            if len(parts) < 4: 
                continue
            data = parts[3].split("^")
            if len(data) < 3: 
                continue
            stock_code, price = data[0], data[2]
            latest_data[stock_code] = {"price": price, "company_name": ticker_name_map.get(stock_code, stock_code)}

# ===================== 프론트 WebSocket 서버 (1분 단위) =====================
clients = set()

async def broadcast_latest_data():
    while True:
        if clients:
            message = []
            for code, info in latest_data.items():
                prev_price = prev_data.get(code)
                curr_price = float(info["price"].replace(",", ""))
                rate = ((curr_price - prev_price) / prev_price * 100) if prev_price else 0
                info_to_send = {
                    "ticker": code,
                    "company_name": info["company_name"],
                    "price": info["price"],
                    "rate": rate
                }
                message.append(info_to_send)
            message_json = json.dumps(message)

            disconnected = set()
            for client in clients:
                try:
                    await client.send(message_json)
                except Exception as e:
                    logging.warning(f"클라이언트 전송 실패, 제거: {e}")
                    disconnected.add(client)
            clients.difference_update(disconnected)

        # 1분마다 전송
        await asyncio.sleep(60)

async def websocket_server(ws):
    clients.add(ws)
    logging.info("✅ 클라이언트 연결됨")

    # ✅ DB에서 초기 데이터 가져와서 즉시 전송
    conn = pymysql.connect(**DB_CONFIG)
    cursor = conn.cursor(pymysql.cursors.DictCursor)
    cursor.execute("SELECT ticker, company_name, price, rate FROM realtime_stock")
    rows = cursor.fetchall()
    cursor.close()
    conn.close()

    if rows:
        await ws.send(json.dumps(rows))
        logging.info("📤 초기 DB 데이터 전송 완료")

    try:
        while True:
            await asyncio.sleep(10)  # 연결 유지
    finally:
        clients.remove(ws)
        logging.info("❌ 클라이언트 연결 종료")

# ===================== 메인 =====================
async def main():
    key = get_approval_key()
    if not key:
        return
    asyncio.create_task(start_koreainvest_ws(key))
    server = await websockets.serve(websocket_server, "localhost", 8765)
    asyncio.create_task(broadcast_latest_data())
    await server.wait_closed()

if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(levelname)s - %(message)s")
    create_table_if_not_exists()
    asyncio.run(main())
