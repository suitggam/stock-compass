package com.stock.survive.serviceImpl;

import com.stock.survive.dto.StockItemOptionDto;
import com.stock.survive.dto.tendency.TendencyGameFinishRequest;
import com.stock.survive.dto.tendency.TendencyGameOrderRequest;
import com.stock.survive.dto.tendency.TendencyGameResultResponse;
import com.stock.survive.dto.tendency.TendencyGameStartRequest;
import com.stock.survive.dto.tendency.TendencyGameStateResponse;
import com.stock.survive.entity.StockInfos;
import com.stock.survive.entity.StockItems;
import com.stock.survive.entity.User;
import com.stock.survive.entity.tendency.TendencyGameNews;
import com.stock.survive.entity.tendency.TendencyGameSession;
import com.stock.survive.entity.tendency.TendencyGameStatus;
import com.stock.survive.entity.tendency.TendencyGameTrade;
import com.stock.survive.entity.tendency.TendencyGameTradeType;
import com.stock.survive.entity.tendency.TendencyGameWeek;
import com.stock.survive.repository.StockInfosRepository;
import com.stock.survive.repository.StockItemsRepository;
import com.stock.survive.repository.UserRepository;
import com.stock.survive.service.TendencyGameService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class TendencyGameServiceImpl implements TendencyGameService {

    private static final int DEFAULT_INITIAL_CASH = 1_000_000;
    private static final int DEFAULT_MAX_WEEK = 10;
    private static final double VOLATILITY_THRESHOLD = 10.0d;
    private static final double YIELD_THRESHOLD = 3.0d;

    private final UserRepository userRepository;
    private final StockItemsRepository stockItemsRepository;
    private final StockInfosRepository stockInfosRepository;

    // 인메모리 세션 저장소 (옵션 A)
    private final ConcurrentHashMap<Long, TendencyGameSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, List<TendencyGameWeek>> weeksBySession = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, List<TendencyGameTrade>> tradesBySession = new ConcurrentHashMap<>();
    private final AtomicLong sessionSeq = new AtomicLong(1L);
    private final AtomicLong tradeSeq = new AtomicLong(1L);

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public TendencyGameStateResponse start(Integer userId, TendencyGameStartRequest request) {
        User user = fetchUser(userId);
        StockItems stockItem = selectStockItem(request);

        List<StockInfos> timeline = stockInfosRepository.findRecent6YearsByTicker(stockItem.getTicker());
        if (timeline.size() < DEFAULT_MAX_WEEK) {
            throw new IllegalStateException("해당 종목의 데이터가 10주차 미만입니다.");
        }

        int startIndex = chooseStartIndex(timeline.size());
        List<StockInfos> selected = timeline.subList(startIndex, startIndex + DEFAULT_MAX_WEEK);

        long newId = sessionSeq.getAndIncrement();
        TendencyGameSession session = TendencyGameSession.builder()
                .id(newId)
                .user(user)
                .ticker(stockItem.getTicker())
                .datasetId(stockItem.getTicker() + "-" + selected.get(0).getDate())
                .companyAlias(generateAlias(stockItem.getCompanyName()))
                .initialCash(DEFAULT_INITIAL_CASH)
                .cash(DEFAULT_INITIAL_CASH)
                .stockQuantity(0)
                .averageCost(0)
                .realizedProfit(0L)
                .currentWeek(1)
                .maxWeek(DEFAULT_MAX_WEEK)
                .status(TendencyGameStatus.IN_PROGRESS)
                .startedAt(LocalDateTime.now())
                .decisionElapsedMillis(0L)
                .volatileBuyCount(0)
                .volatileSellCount(0)
                .sellDominantWeekCount(0)
                .yieldAboveThreshold(false)
                .build();

        buildWeeks(session, selected);
        sessions.put(session.getId(), session);

        return buildStateResponse(session);
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public TendencyGameStateResponse getState(Integer userId, Long sessionId) {
        TendencyGameSession session = fetchSession(userId, sessionId);
        return buildStateResponse(session);
    }

    @Override
    public TendencyGameStateResponse placeOrder(Integer userId, Long sessionId, TendencyGameOrderRequest request) {
        TendencyGameSession session = fetchSession(userId, sessionId);
        ensureInProgress(session);

        TendencyGameWeek currentWeek = currentWeek(session);
        int price = safePrice(currentWeek.getClosePrice());

        if (request.type() == TendencyGameTradeType.BUY) {
            applyBuy(session, request.quantity(), price);
        } else {
            applySell(session, request.quantity(), price);
        }

        boolean volatileContext = Math.abs(currentWeek.getChangeRate()) >= VOLATILITY_THRESHOLD;
        if (volatileContext) {
            if (request.type() == TendencyGameTradeType.BUY) {
                session.setVolatileBuyCount(session.getVolatileBuyCount() + 1);
            } else {
                session.setVolatileSellCount(session.getVolatileSellCount() + 1);
            }
        }

        TendencyGameTrade trade = TendencyGameTrade.builder()
                .id(tradeSeq.getAndIncrement())
                .session(session)
                .type(request.type())
                .price(price)
                .quantity(request.quantity())
                .weekIndex(session.getCurrentWeek())
                .executedAt(LocalDateTime.now())
                .executedDate(LocalDate.now())
                .volatilityContext(volatileContext)
                .build();
        getTrades(session.getId()).add(trade);

        // 세션 갱신 저장
        sessions.put(session.getId(), session);
        return buildStateResponse(session);
    }

    @Override
    public TendencyGameStateResponse proceedNextWeek(Integer userId, Long sessionId) {
        TendencyGameSession session = fetchSession(userId, sessionId);
        ensureInProgress(session);

        if (session.getCurrentWeek() >= session.getMaxWeek()) {
            throw new IllegalStateException("이미 마지막 주차입니다. 다음 주로 이동할 수 없습니다.");
        }

        updateSellDominantMetric(session, session.getCurrentWeek());
        session.setCurrentWeek(session.getCurrentWeek() + 1);
        session.setDecisionElapsedMillis(Duration.between(session.getStartedAt(), LocalDateTime.now()).toMillis());
        sessions.put(session.getId(), session);
        return buildStateResponse(session);
    }

    @Override
    public TendencyGameResultResponse finish(Integer userId, TendencyGameFinishRequest request) {
        TendencyGameSession session = fetchSession(userId, request.sessionId());
        ensureInProgress(session);

        if (!session.getCurrentWeek().equals(session.getMaxWeek())) {
            throw new IllegalStateException("10주차까지 진행해야 결과를 확인할 수 있습니다.");
        }

        updateSellDominantMetric(session, session.getCurrentWeek());
        List<TendencyGameWeek> weeks = getWeeks(session.getId());
        if (weeks.size() < session.getMaxWeek()) {
            throw new IllegalStateException("주간 데이터가 올바르지 않습니다.");
        }

        TendencyGameWeek finalWeek = weeks.get(session.getMaxWeek() - 1);
        int finalPrice = safePrice(finalWeek.getClosePrice());
        long stockValuation = (long) session.getStockQuantity() * finalPrice;
        long totalAsset = session.getCash() + stockValuation;
        double totalYield = calculateYield(session.getInitialCash(), totalAsset);

        List<TendencyGameTrade> trades = getTrades(session.getId());
        int volatilityBuy = (int) trades.stream()
                .filter(trade -> Boolean.TRUE.equals(trade.getVolatilityContext()))
                .filter(trade -> trade.getType() == TendencyGameTradeType.BUY)
                .count();
        int volatilitySell = (int) trades.stream()
                .filter(trade -> Boolean.TRUE.equals(trade.getVolatilityContext()))
                .filter(trade -> trade.getType() == TendencyGameTradeType.SELL)
                .count();

        int sellDominantWeeks = countSellDominantWeeks(session.getMaxWeek(), trades);

        session.setVolatileBuyCount(volatilityBuy);
        session.setVolatileSellCount(volatilitySell);
        session.setSellDominantWeekCount(sellDominantWeeks);
        session.setYieldAboveThreshold(totalYield >= YIELD_THRESHOLD);
        session.setFinishedAt(LocalDateTime.now());
        session.setDecisionElapsedMillis(Duration.between(session.getStartedAt(), session.getFinishedAt()).toMillis());
        session.setStatus(TendencyGameStatus.FINISHED);

        TendencyProfile profile = resolveTendencyProfile(totalYield, volatilityBuy + volatilitySell, sellDominantWeeks);
        session.setTendencyType(profile.type());
        session.setRecommendation(profile.recommendation());
        sessions.put(session.getId(), session);

        return new TendencyGameResultResponse(
                session.getId(),
                session.getMaxWeek(),
                session.getCurrentWeek(),
                Math.toIntExact(totalAsset),
                session.getRealizedProfit(),
                totalYield,
                session.getYieldAboveThreshold(),
                session.getTendencyType(),
                session.getRecommendation(),
                session.getDecisionElapsedMillis() / 1000,
                session.getVolatileBuyCount(),
                session.getVolatileSellCount(),
                session.getSellDominantWeekCount(),
                session.getStartedAt(),
                session.getFinishedAt()
        );
    }

    // ===== In-memory helpers =====
    private List<TendencyGameWeek> getWeeks(Long sessionId) {
        return weeksBySession.computeIfAbsent(sessionId, k -> new ArrayList<>());
    }

    private List<TendencyGameTrade> getTrades(Long sessionId) {
        return tradesBySession.computeIfAbsent(sessionId, k -> new ArrayList<>());
    }

    private List<TendencyGameTrade> getTradesByWeek(Long sessionId, int weekIndex) {
        return getTrades(sessionId).stream()
                .filter(t -> t.getWeekIndex() == weekIndex)
                .sorted(Comparator.comparing(TendencyGameTrade::getExecutedAt))
                .collect(Collectors.toList());
    }

    private void buildWeeks(TendencyGameSession session, List<StockInfos> selected) {
        List<TendencyGameWeek> list = new ArrayList<>();
        for (int i = 0; i < selected.size(); i++) {
            StockInfos current = selected.get(i);
            int closePrice = safePrice(Optional.ofNullable(current.getEndPrice()).orElse(0));
            int previousPrice = closePrice;
            if (i > 0) {
                previousPrice = safePrice(Optional.ofNullable(selected.get(i - 1).getEndPrice()).orElse(closePrice));
            }
            int change = closePrice - previousPrice;
            double changeRate = previousPrice == 0 ? 0.0 : (change * 100.0) / previousPrice;

            TendencyGameWeek week = TendencyGameWeek.builder()
                    .session(session)
                    .weekIndex(i + 1)
                    .startDate(current.getDate())
                    .endDate(current.getDate().plusDays(6))
                    .closePrice(closePrice)
                    .changePrice(change)
                    .changeRate(changeRate)
                    .keywords(generateKeywords(i + 1))
                    .news(generateNews(current.getDate(), i + 1))
                    .build();
            list.add(week);
        }
        weeksBySession.put(session.getId(), list);
    }

    private List<String> generateKeywords(int weekIndex) {
        List<String> keywords = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            keywords.add("Keyword " + weekIndex + "-" + i);
        }
        return keywords;
    }

    private List<TendencyGameNews> generateNews(LocalDate baseDate, int weekIndex) {
        List<TendencyGameNews> news = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            news.add(TendencyGameNews.builder()
                    .title("Week " + weekIndex + " 뉴스 " + i)
                    .url("https://example.com/news/" + weekIndex + "/" + i)
                    .summary(baseDate + " 관련 요약 " + i)
                    .build());
        }
        return news;
    }

    private String generateAlias(String companyName) {
        char suffix = (char) (ThreadLocalRandom.current().nextInt(0, 26) + 'A');
        return "익명 기업 " + suffix;
    }

    private int chooseStartIndex(int timelineSize) {
        int maxStart = timelineSize - DEFAULT_MAX_WEEK;
        if (maxStart <= 0) {
            return 0;
        }
        return ThreadLocalRandom.current().nextInt(0, maxStart + 1);
    }

    private StockItems selectStockItem(TendencyGameStartRequest request) {
        if (request != null) {
            if (StringUtils.hasText(request.ticker())) {
                return entityManager.createQuery("SELECT si FROM StockItems si WHERE si.ticker = :ticker", StockItems.class)
                        .setParameter("ticker", request.ticker().toUpperCase(Locale.ROOT))
                        .setMaxResults(1)
                        .getResultStream()
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("ticker에 해당하는 종목이 없습니다."));
            }
            if (request.itemNo() != null) {
                return stockItemsRepository.findById(request.itemNo())
                        .orElseThrow(() -> new IllegalArgumentException("itemNo에 해당하는 종목이 없습니다."));
            }
        }

        List<StockItemOptionDto> options = stockItemsRepository.findAllOptions();
        if (options.isEmpty()) {
            throw new IllegalStateException("등록된 종목이 없습니다.");
        }
        List<StockItemOptionDto> candidates = options.size() > 50 ? options.subList(0, 50) : options;
        StockItemOptionDto picked = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        return stockItemsRepository.findById(picked.getItemNo())
                .orElseThrow(() -> new IllegalStateException("선택된 종목을 조회할 수 없습니다."));
    }

    private User fetchUser(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    private TendencyGameSession fetchSession(Integer userId, Long sessionId) {
        TendencyGameSession s = sessions.get(sessionId);
        if (s == null || s.getUser() == null || !s.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("진행 중인 게임 세션을 찾을 수 없습니다.");
        }
        return s;
    }

    private void ensureInProgress(TendencyGameSession session) {
        if (session.getStatus() != TendencyGameStatus.IN_PROGRESS) {
            throw new IllegalStateException("종료된 게임입니다. 작업을 수행할 수 없습니다.");
        }
    }

    private TendencyGameWeek currentWeek(TendencyGameSession session) {
        List<TendencyGameWeek> weeks = getWeeks(session.getId());
        if (weeks.size() < session.getCurrentWeek()) {
            throw new IllegalStateException("주간 데이터가 부족합니다.");
        }
        return weeks.get(session.getCurrentWeek() - 1);
    }

    private void applyBuy(TendencyGameSession session, int quantity, int price) {
        long cost = (long) quantity * price;
        if (cost > session.getCash()) {
            throw new IllegalArgumentException("보유 현금이 부족합니다.");
        }
        long remaining = session.getCash() - cost;
        if (remaining < 0 || remaining > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("현금 계산 중 오버플로우가 발생했습니다.");
        }

        int previousQuantity = session.getStockQuantity();
        int newQuantity = previousQuantity + quantity;
        long totalCostBefore = (long) previousQuantity * session.getAverageCost();
        int newAverage = newQuantity == 0 ? 0 : (int) Math.round((double) (totalCostBefore + cost) / newQuantity);

        session.setCash((int) remaining);
        session.setStockQuantity(newQuantity);
        session.setAverageCost(newAverage);
    }

    private void applySell(TendencyGameSession session, int quantity, int price) {
        if (session.getStockQuantity() < quantity) {
            throw new IllegalArgumentException("보유 수량보다 많이 매도할 수 없습니다.");
        }
        long revenue = (long) quantity * price;
        long newCash = session.getCash() + revenue;
        if (newCash > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("현금 계산 중 오버플로우가 발생했습니다.");
        }

        int remaining = session.getStockQuantity() - quantity;
        long profit = (long) (price - session.getAverageCost()) * quantity;

        session.setCash((int) newCash);
        session.setStockQuantity(remaining);
        session.setRealizedProfit(session.getRealizedProfit() + profit);
        if (remaining == 0) {
            session.setAverageCost(0);
        }
    }

    private void updateSellDominantMetric(TendencyGameSession session, int weekIndex) {
        List<TendencyGameTrade> trades = getTradesByWeek(session.getId(), weekIndex);
        if (trades.isEmpty()) {
            return;
        }
        int buyQty = trades.stream()
                .filter(trade -> trade.getType() == TendencyGameTradeType.BUY)
                .mapToInt(TendencyGameTrade::getQuantity)
                .sum();
        int sellQty = trades.stream()
                .filter(trade -> trade.getType() == TendencyGameTradeType.SELL)
                .mapToInt(TendencyGameTrade::getQuantity)
                .sum();
        if (sellQty > buyQty) {
            session.setSellDominantWeekCount(session.getSellDominantWeekCount() + 1);
        }
    }

    private int countSellDominantWeeks(int maxWeek, List<TendencyGameTrade> trades) {
        int count = 0;
        for (int week = 1; week <= maxWeek; week++) {
            int buyQty = trades.stream()
                    .filter(trade -> trade.getWeekIndex() == week)
                    .filter(trade -> trade.getType() == TendencyGameTradeType.BUY)
                    .mapToInt(TendencyGameTrade::getQuantity)
                    .sum();
            int sellQty = trades.stream()
                    .filter(trade -> trade.getWeekIndex() == week)
                    .filter(trade -> trade.getType() == TendencyGameTradeType.SELL)
                    .mapToInt(TendencyGameTrade::getQuantity)
                    .sum();
            if (sellQty > buyQty) {
                count++;
            }
        }
        return count;
    }

    private int safePrice(Integer value) {
        return value == null ? 0 : value;
    }

    private double calculateYield(int initialCash, long totalAsset) {
        if (initialCash == 0) {
            return 0.0;
        }
        return ((double) totalAsset - initialCash) / initialCash * 100.0;
    }

    private TendencyProfile resolveTendencyProfile(double totalYield, int volatileTrades, int sellDominantWeeks) {
        if (totalYield >= 5.0 || volatileTrades >= 6) {
            return new TendencyProfile("AGGRESSIVE", "공격적인 성향으로 적극적인 투자를 선호합니다.");
        }
        if (sellDominantWeeks >= 5 || totalYield < 0) {
            return new TendencyProfile("DEFENSIVE", "안정성을 중시하며 위험을 회피하는 성향입니다.");
        }
        return new TendencyProfile("BALANCED", "수익과 리스크를 균형 있게 고려하는 성향입니다.");
    }

    private record TendencyProfile(String type, String recommendation) {
    }
}
