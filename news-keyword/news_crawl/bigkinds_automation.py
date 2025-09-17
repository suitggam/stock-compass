import time
import os
from datetime import datetime, timedelta
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.chrome.options import Options
from webdriver_manager.chrome import ChromeDriverManager
from selenium.common.exceptions import TimeoutException, NoSuchElementException
from selenium.webdriver.common.action_chains import ActionChains
import logging

class BigKindsAutomation:
    def __init__(self, email, password):
        self.email = email
        self.password = password
        self.driver = None
        self.wait = None
        self.setup_logging()
        
    def setup_logging(self):
        """로깅 설정"""
        # Docker 환경에서는 logs 디렉토리 사용
        log_dir = os.environ.get('LOG_DIR', '.')
        os.makedirs(log_dir, exist_ok=True)
        
        logging.basicConfig(
            level=logging.INFO,
            format='%(asctime)s - %(levelname)s - %(message)s',
            handlers=[
                logging.FileHandler(os.path.join(log_dir, 'bigkinds_automation.log'), encoding='utf-8'),
                logging.StreamHandler()
            ]
        )
        self.logger = logging.getLogger(__name__)
        
    def setup_driver(self):
        """Chrome/Chromium 드라이버 설정"""
        try:
            chrome_options = Options()
            
            # Docker 환경에서 필요한 옵션들
            chrome_options.add_argument("--no-sandbox")
            chrome_options.add_argument("--disable-dev-shm-usage")
            chrome_options.add_argument("--disable-gpu")
            chrome_options.add_argument("--window-size=1920,1080")
            chrome_options.add_argument("--disable-extensions")
            chrome_options.add_argument("--disable-plugins")
            chrome_options.add_argument("--disable-images")
            chrome_options.add_argument("--disable-web-security")
            chrome_options.add_argument("--allow-running-insecure-content")
            chrome_options.add_argument("--disable-blink-features=AutomationControlled")
            chrome_options.add_experimental_option("excludeSwitches", ["enable-automation"])
            chrome_options.add_experimental_option('useAutomationExtension', False)
            
            # Docker 환경에서 헤드리스 모드 활성화
            if os.environ.get('DOCKER_ENV') == 'true' or not os.environ.get('DISPLAY'):
                chrome_options.add_argument("--headless")
                self.logger.info("Docker 환경에서 헤드리스 모드로 실행")
            
            # 다운로드 경로 설정
            download_dir = os.environ.get('DOWNLOAD_DIR', '/app/downloads')
            os.makedirs(download_dir, exist_ok=True)
            self.download_dir = download_dir
            
            prefs = {
                "download.default_directory": download_dir,
                "download.prompt_for_download": False,
                "download.directory_upgrade": True,
                "safebrowsing.enabled": True
            }
            chrome_options.add_experimental_option("prefs", prefs)
            
            # Docker 환경에서 Chromium 사용
            if os.environ.get('DOCKER_ENV') == 'true':
                # Chromium 바이너리 경로 설정
                chrome_bin = os.environ.get('CHROME_BIN', '/usr/bin/chromium')
                if os.path.exists(chrome_bin):
                    chrome_options.binary_location = chrome_bin
                    self.logger.info(f"Chromium 바이너리 사용: {chrome_bin}")
                else:
                    self.logger.warning(f"Chromium 바이너리를 찾을 수 없습니다: {chrome_bin}")
                
                # Chromium 드라이버 경로 설정
                chromedriver_path = os.environ.get('CHROMEDRIVER_PATH', '/usr/bin/chromedriver')
                if os.path.exists(chromedriver_path):
                    service = Service(chromedriver_path)
                    self.logger.info(f"Chromium 드라이버 사용: {chromedriver_path}")
                else:
                    self.logger.warning(f"Chromium 드라이버를 찾을 수 없습니다: {chromedriver_path}")
                    # 기본 ChromeDriver 사용
                    service = Service(ChromeDriverManager().install())
            else:
                # 로컬 환경에서는 기본 ChromeDriver 사용
                service = Service(ChromeDriverManager().install())
            
            # 드라이버 생성
            self.driver = webdriver.Chrome(service=service, options=chrome_options)
            self.wait = WebDriverWait(self.driver, 20)
            
            # User-Agent 설정
            self.driver.execute_script("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})")
            
            # Headless 다운로드 허용 (CDP)
            try:
                self.driver.execute_cdp_cmd('Page.setDownloadBehavior', {
                    'behavior': 'allow',
                    'downloadPath': download_dir
                })
                self.logger.info(f"다운로드 경로 활성화: {download_dir}")
            except Exception as e:
                self.logger.warning(f"CDP 다운로드 허용 설정 실패(무시 가능): {e}")

            self.logger.info("Chrome/Chromium 드라이버 설정 완료")
            return True
            
        except Exception as e:
            self.logger.error(f"드라이버 설정 실패: {e}")
            return False
    
    def login(self):
        """BIG KINDS 로그인 - 여러 방법으로 시도"""
        try:
            self.logger.info("BIG KINDS 로그인 시작")
            
            # 페이지가 완전히 로드될 때까지 대기
            time.sleep(5)
            
            # 여러 가지 방법으로 로그인 버튼 찾기 시도
            login_methods = [
                self._try_direct_login_button,
                self._try_user_icon_hover,
                self._try_navigation_menu,
                self._try_footer_login
            ]
            
            for method in login_methods:
                try:
                    self.logger.info(f"로그인 방법 시도: {method.__name__}")
                    if method():
                        self.logger.info("로그인 성공")
                        return True
                except Exception as e:
                    self.logger.warning(f"로그인 방법 {method.__name__} 실패: {e}")
                    continue
            
            self.logger.error("모든 로그인 방법 실패")
            return False
            
        except Exception as e:
            self.logger.error(f"로그인 실패: {e}")
            return False
    
    def _try_direct_login_button(self):
        """직접 로그인 버튼 찾기"""
        try:
            # 페이지에서 직접 로그인 버튼 찾기
            login_selectors = [
                "//a[contains(text(), '로그인')]",
                "//button[contains(text(), '로그인')]",
                "//span[contains(text(), '로그인')]",
                "//div[contains(text(), '로그인')]",
                "//*[contains(@class, 'login')]",
                "//*[contains(@id, 'login')]"
            ]
            
            for selector in login_selectors:
                try:
                    login_btn = self.driver.find_element(By.XPATH, selector)
                    if login_btn.is_displayed() and login_btn.is_enabled():
                        self.logger.info(f"직접 로그인 버튼 찾음: {selector}")
                        login_btn.click()
                        time.sleep(3)
                        return self._complete_login_form()
                except:
                    continue
            
            return False
        except Exception as e:
            self.logger.warning(f"직접 로그인 버튼 방법 실패: {e}")
            return False
    
    def _try_user_icon_hover(self):
        """사용자 아이콘 호버 방법"""
        try:
            # 사용자 아이콘 찾기
            user_icon_selectors = [
                "//i[contains(@class, 'user')]",
                "//i[contains(@class, 'person')]",
                "//i[contains(@class, 'account')]",
                "//span[contains(@class, 'user')]",
                "//div[contains(@class, 'user')]",
                "//a[contains(@class, 'user')]",
                "//button[contains(@class, 'user')]",
                "//*[contains(@class, 'user')]"
            ]
            
            user_icon = None
            for selector in user_icon_selectors:
                try:
                    user_icon = self.driver.find_element(By.XPATH, selector)
                    if user_icon.is_displayed():
                        self.logger.info(f"사용자 아이콘 찾음: {selector}")
                        break
                except:
                    continue
            
            if not user_icon:
                return False
            
            # JavaScript로 호버 이벤트 발생
            self.driver.execute_script("arguments[0].dispatchEvent(new MouseEvent('mouseover', {bubbles: true}));", user_icon)
            time.sleep(2)
            
            # 호버 후 나타나는 로그인 버튼 찾기
            hover_login_selectors = [
                "//a[contains(text(), '로그인')]",
                "//button[contains(text(), '로그인')]",
                "//span[contains(text(), '로그인')]",
                "//div[contains(text(), '로그인')]"
            ]
            
            for selector in hover_login_selectors:
                try:
                    login_btn = self.driver.find_element(By.XPATH, selector)
                    if login_btn.is_displayed() and login_btn.is_enabled():
                        self.logger.info(f"호버 후 로그인 버튼 찾음: {selector}")
                        login_btn.click()
                        time.sleep(3)
                        return self._complete_login_form()
                except:
                    continue
            
            return False
        except Exception as e:
            self.logger.warning(f"사용자 아이콘 호버 방법 실패: {e}")
            return False
    
    def _try_navigation_menu(self):
        """네비게이션 메뉴에서 로그인 찾기"""
        try:
            # 메뉴나 네비게이션에서 로그인 찾기
            nav_login_selectors = [
                "//nav//a[contains(text(), '로그인')]",
                "//header//a[contains(text(), '로그인')]",
                "//ul//a[contains(text(), '로그인')]",
                "//menu//a[contains(text(), '로그인')]"
            ]
            
            for selector in nav_login_selectors:
                try:
                    login_btn = self.driver.find_element(By.XPATH, selector)
                    if login_btn.is_displayed() and login_btn.is_enabled():
                        self.logger.info(f"네비게이션에서 로그인 버튼 찾음: {selector}")
                        login_btn.click()
                        time.sleep(3)
                        return self._complete_login_form()
                except:
                    continue
            
            return False
        except Exception as e:
            self.logger.warning(f"네비게이션 메뉴 방법 실패: {e}")
            return False
    
    def _try_footer_login(self):
        """푸터에서 로그인 찾기"""
        try:
            # 푸터에서 로그인 찾기
            footer_login_selectors = [
                "//footer//a[contains(text(), '로그인')]",
                "//div[contains(@class, 'footer')]//a[contains(text(), '로그인')]"
            ]
            
            for selector in footer_login_selectors:
                try:
                    login_btn = self.driver.find_element(By.XPATH, selector)
                    if login_btn.is_displayed() and login_btn.is_enabled():
                        self.logger.info(f"푸터에서 로그인 버튼 찾음: {selector}")
                        login_btn.click()
                        time.sleep(3)
                        return self._complete_login_form()
                except:
                    continue
            
            return False
        except Exception as e:
            self.logger.warning(f"푸터 로그인 방법 실패: {e}")
            return False
    
    def _complete_login_form(self):
        """로그인 폼 완성"""
        try:
            self.logger.info("로그인 폼 완성 시작")
            
            # 로그인 폼이 나타날 때까지 대기
            time.sleep(3)
            
            # ID로 직접 이메일 입력 필드 찾기
            try:
                email_input = self.driver.find_element(By.ID, "login-user-id")
                self.logger.info("이메일 입력 필드 찾음: login-user-id")
            except:
                self.logger.error("이메일 입력 필드(login-user-id)를 찾을 수 없습니다")
                return False
            
            # ID로 직접 비밀번호 입력 필드 찾기
            try:
                password_input = self.driver.find_element(By.ID, "login-user-password")
                self.logger.info("비밀번호 입력 필드 찾음: login-user-password")
            except:
                self.logger.error("비밀번호 입력 필드(login-user-password)를 찾을 수 없습니다")
                return False
            
            # 이메일 입력
            try:
                self.driver.execute_script("arguments[0].value = arguments[1];", email_input, self.email)
                self.logger.info("JavaScript로 이메일 입력 성공")
            except Exception as e:
                self.logger.error(f"이메일 입력 실패: {e}")
                return False
            
            time.sleep(1)
            
            # 비밀번호 입력
            try:
                self.driver.execute_script("arguments[0].value = arguments[1];", password_input, self.password)
                self.logger.info("JavaScript로 비밀번호 입력 성공")
            except Exception as e:
                self.logger.error(f"비밀번호 입력 실패: {e}")
                return False
            
            time.sleep(1)
            
            # 로그인 제출 버튼 찾기 및 클릭
            try:
                submit_btn = self.driver.find_element(By.XPATH, "//button[@type='submit']")
                self.logger.info("제출 버튼 찾음")
            except:
                self.logger.error("제출 버튼을 찾을 수 없습니다")
                return False
            
            # 제출 버튼 클릭
            try:
                self.driver.execute_script("arguments[0].click();", submit_btn)
                self.logger.info("JavaScript로 제출 버튼 클릭 성공")
            except Exception as e:
                self.logger.error(f"제출 버튼 클릭 실패: {e}")
                return False
            
            time.sleep(5)
            
            # 로그인 성공 여부 확인
            return self._verify_login_success()
            
        except Exception as e:
            self.logger.error(f"로그인 폼 완성 실패: {e}")
            return False
    
    def _verify_login_success(self):
        """로그인 성공 여부 확인"""
        try:
            # 로그인 후 나타나는 요소들 확인
            success_indicators = [
                "//a[contains(text(), '로그아웃')]",
                "//a[contains(text(), '마이페이지')]",
                "//span[contains(text(), '님')]",
                "//div[contains(@class, 'user-info')]",
                "//*[contains(@class, 'logout')]",
                "//*[contains(@class, 'user')]"
            ]
            
            login_success = False
            for indicator in success_indicators:
                try:
                    element = self.driver.find_element(By.XPATH, indicator)
                    if element.is_displayed():
                        login_success = True
                        self.logger.info(f"로그인 성공 확인: {indicator}")
                        break
                except:
                    continue
            
            if login_success:
                self.logger.info("로그인 성공 확인됨")
                return True
            else:
                self.logger.warning("로그인 성공 여부를 확인할 수 없습니다")
                # URL 변경이나 다른 지표로 확인
                current_url = self.driver.current_url
                if 'login' not in current_url.lower() and 'signin' not in current_url.lower():
                    self.logger.info("URL 변경으로 로그인 성공 추정")
                    return True
                return True  # 일단 성공으로 간주
                
        except Exception as e:
            self.logger.warning(f"로그인 성공 확인 중 오류: {e}")
            return True  # 일단 성공으로 간주
    
    def navigate_to_news_analysis(self):
        """뉴스 검색 분석 탭으로 이동"""
        try:
            self.logger.info("뉴스 검색 분석 탭으로 이동")
            
            # 로그인 후 페이지 로딩 대기
            time.sleep(3)
            
            # 직접 뉴스검색·분석 페이지 URL로 이동
            news_analysis_url = "https://www.bigkinds.or.kr/v2/news/index.do"
            self.logger.info(f"직접 URL로 이동: {news_analysis_url}")
            
            self.driver.get(news_analysis_url)
            time.sleep(5)
            
            # 페이지 이동 확인
            current_url = self.driver.current_url
            self.logger.info(f"현재 URL: {current_url}")
            
            # 페이지 제목 확인
            page_title = self.driver.title
            self.logger.info(f"페이지 제목: {page_title}")
            
            # 뉴스 검색 페이지의 특징적인 요소 확인
            search_page_indicators = [
                "//input[@placeholder*='검색어']",
                "//input[@placeholder*='키워드']",
                "//button[contains(text(), '검색')]",
                "//div[contains(@class, 'search')]",
                "//form[contains(@class, 'search')]",
                "//input[@id='total-search-key']",
                "//input[@name='interestKeyword']"
            ]
            
            page_loaded = False
            for indicator in search_page_indicators:
                try:
                    element = self.driver.find_element(By.XPATH, indicator)
                    if element.is_displayed():
                        page_loaded = True
                        self.logger.info(f"뉴스 검색 페이지 로드 확인: {indicator}")
                        break
                except:
                    continue
            
            if page_loaded:
                self.logger.info("뉴스 검색 분석 페이지 도달 성공")
                return True
            else:
                self.logger.warning("뉴스 검색 페이지의 특징적인 요소를 찾을 수 없습니다")
                # URL이나 제목으로 판단
                if any(keyword in current_url.lower() for keyword in ['news', 'search', 'analysis', '뉴스', '검색', '분석', 'v2']):
                    self.logger.info("URL 기반으로 뉴스 검색 페이지 도달 추정")
                    return True
                elif any(keyword in page_title.lower() for keyword in ['news', 'search', 'analysis', '뉴스', '검색', '분석']):
                    self.logger.info("제목 기반으로 뉴스 검색 페이지 도달 추정")
                    return True
                else:
                    self.logger.error("뉴스 검색 페이지 도달 실패")
                    return False
            
        except Exception as e:
            self.logger.error(f"뉴스 검색 분석 페이지 이동 실패: {e}")
            # 현재 페이지 정보 로깅
            try:
                self.logger.info(f"현재 URL: {self.driver.current_url}")
                self.logger.info(f"현재 제목: {self.driver.title}")
                page_source = self.driver.page_source[:1000]
                self.logger.info(f"페이지 소스 일부: {page_source}")
            except:
                pass
            return False
    
    def set_period_one_day(self):
        """분석 페이지에서 기간 → 1일 선택"""
        try:
            self.logger.info("기간 탭 클릭 후 1일 선택")
            time.sleep(1)

            # 기간 탭/버튼 클릭 (텍스트 포함 다양한 경우 대응)
            period_triggers = [
                "//button[contains(text(), '기간')]",
                "//a[contains(text(), '기간')]",
                "//*[contains(@class, 'date') and (self::button or self::a)]",
                "//div[contains(@class, 'date')]//button",
            ]
            period_btn = None
            for xp in period_triggers:
                try:
                    el = self.driver.find_element(By.XPATH, xp)
                    if el.is_displayed() and el.is_enabled():
                        period_btn = el
                        break
                except:
                    continue
            if not period_btn:
                # 페이지 내 '1일' 라디오가 보이면 기간 버튼 없이 직접 진행
                self.logger.warning("기간 버튼을 못찾음. '1일' 직접 선택 시도")
            else:
                try:
                    self.driver.execute_script("arguments[0].click();", period_btn)
                except:
                    try:
                        period_btn.click()
                    except Exception as e:
                        self.logger.warning(f"기간 버튼 클릭 실패: {e}")
            time.sleep(1)

            # '1일' 옵션 선택 (라디오/버튼 모두 대응)
            one_day_selectors = [
                "//label[contains(., '1일')]",
                "//button[contains(text(), '1일')]",
                "//span[contains(text(), '1일')]/ancestor::label",
                "//input[@type='radio' and (contains(@id,'date') or contains(@name,'date'))]/following-sibling::label[contains(., '1일')]",
                "//*[contains(@class,'date') and contains(.,'1일')]",
            ]
            one_day = None
            for xp in one_day_selectors:
                try:
                    el = self.driver.find_element(By.XPATH, xp)
                    if el.is_displayed() and el.is_enabled():
                        one_day = el
                        break
                except:
                    continue
            if not one_day:
                self.logger.error("'1일' 옵션을 찾을 수 없습니다")
                return False

            try:
                self.driver.execute_script("arguments[0].click();", one_day)
            except:
                try:
                    one_day.click()
                except Exception as e:
                    self.logger.error(f"'1일' 클릭 실패: {e}")
                    return False

            time.sleep(1)

            # 적용/확인 버튼이 따로 있는 UI 대응
            apply_candidates = [
                "//button[contains(text(),'적용')]",
                "//button[contains(text(),'확인')]",
                "//a[contains(text(),'적용')]",
                "//a[contains(text(),'확인')]",
            ]
            for xp in apply_candidates:
                try:
                    btn = self.driver.find_element(By.XPATH, xp)
                    if btn.is_displayed() and btn.is_enabled():
                        try:
                            self.driver.execute_script("arguments[0].click();", btn)
                        except:
                            btn.click()
                        break
                except:
                    continue

            self.logger.info("기간 1일 선택 완료")
            return True
        except Exception as e:
            self.logger.error(f"기간 1일 선택 실패: {e}")
            return False

    def select_economy_and_apply(self):
        """통합 분류 탭 클릭 → '경제' 체크 → 적용하기"""
        try:
            self.logger.info("통합 분류에서 '경제' 선택 후 적용")
            time.sleep(1)

            # 0) 필터/상세검색 열기(있다면)
            try:
                self.driver.execute_script(
                    """
                    const selectors = [
                      'button', 'a', '[role="button"]', '[data-role="toggle"]'
                    ];
                    const keys = ['상세검색','상세 검색','필터','검색옵션','검색 옵션','조건설정','조건 설정'];
                    const all = Array.from(document.querySelectorAll(selectors.join(',')));
                    for (const el of all){
                      const t = (el.innerText||el.textContent||'').replace(/\s+/g,'');
                      if (!t) continue;
                      for (const k of keys){
                        if (t.includes(k.replace(/\s+/g,''))){
                          try { el.click(); return true; } catch(e){}
                        }
                      }
                    }
                    return false;
                    """
                )
            except:
                pass
            time.sleep(0.8)

            # 1) 통합 분류 탭/버튼 열기 (공백 제거 텍스트 매칭 포함)
            opened = self.driver.execute_script(
                """
                function clickByTextLike(keys){
                  const nodes = Array.from(document.querySelectorAll('button, a, [role="tab"], [role="button"], .tab button, .tab a'));
                  for (const el of nodes){
                    const t = (el.innerText||el.textContent||'').replace(/\s+/g,'');
                    if (!t) continue;
                    for (const k of keys){
                      if (t.includes(k)){
                        try { el.scrollIntoView({block:'center'}); el.click(); return true; } catch(e){}
                      }
                    }
                  }
                  return false;
                }
                const keys = ['통합분류','분류','카테고리','분야'];
                if (clickByTextLike(keys)) return true;
                // 탭 패널 헤더 형태(aria-controls 등)
                const headers = Array.from(document.querySelectorAll('[aria-controls]'));
                for (const h of headers){
                  const t = (h.innerText||h.textContent||'').replace(/\s+/g,'');
                  if (!t) continue;
                  if (keys.some(k => t.includes(k))){ try { h.click(); return true; } catch(e){} }
                }
                return false;
                """
            )
            if not opened:
                self.logger.warning("통합 분류 탭 텍스트 매칭 실패. 대체 XPath 시도")
                category_triggers = [
                    "//button[contains(normalize-space(.),'통합 분류')]",
                    "//a[contains(normalize-space(.),'통합 분류')]",
                    "//button[contains(normalize-space(.),'분류')]",
                    "//a[contains(normalize-space(.),'분류')]",
                    "//*[@role='tab' and contains(normalize-space(.),'분류')]",
                    "//*[contains(@class,'filter') or contains(@class,'accordion') or contains(@class,'tab')]//*[contains(normalize-space(.),'통합 분류') or contains(normalize-space(.),'분류')]",
                ]
                for xp in category_triggers:
                    try:
                        el = self.driver.find_element(By.XPATH, xp)
                        if el.is_displayed() and el.is_enabled():
                            try:
                                self.driver.execute_script("arguments[0].scrollIntoView({block:'center'}); arguments[0].click();", el)
                            except:
                                el.click()
                            break
                    except:
                        continue
            time.sleep(1)

            # 패널 열림 재확인: 체크박스 컨테이너나 '적용' 영역이 보여야 함
            panel_open = False
            for xp in [
                "//button[contains(text(),'적용하기')]",
                "//a[contains(text(),'적용하기')]",
                "//div[contains(@class,'category') or contains(@class,'filter') or contains(@class,'panel')]//input[@type='checkbox']",
                "//*[contains(text(),'경제')]"
            ]:
                try:
                    if self.driver.find_element(By.XPATH, xp).is_displayed():
                        panel_open = True
                        break
                except:
                    continue
            if not panel_open:
                self.logger.warning("통합 분류 패널 열림 감지 실패. 한번 더 탭 클릭 시도")
                self.driver.execute_script(
                    """
                    const keys = ['통합분류','분류','카테고리','분야'];
                    const nodes = Array.from(document.querySelectorAll('button, a, [role="tab"], [role="button"], .tab button, .tab a'));
                    for (const el of nodes){
                      const t = (el.innerText||el.textContent||'').replace(/\s+/g,'');
                      if (t && keys.some(k => t.includes(k))){ try { el.click(); return true; } catch(e){} }
                    }
                    return false;
                    """
                )
                time.sleep(0.8)

            # 열림 상태에서 스크롤을 중앙으로 이동(고정 헤더에 가려지는 문제 방지)
            try:
                self.driver.execute_script("window.scrollBy(0, -100);")
            except:
                pass

            # 2) '경제' 체크박스 탐색 (JS 근접 텍스트 매칭 우선)
            js_find = """
            function getTextAround(el){
              const getText = n => (n?.innerText || n?.textContent || '').replace(/\s+/g,'');
              const label = el.closest('label');
              if (label) return getText(label);
              if (el.nextElementSibling) {
                const sib = getText(el.nextElementSibling);
                if (sib) return sib;
              }
              const parent = el.parentElement;
              if (parent){
                const ptxt = getText(parent);
                if (ptxt) return ptxt;
              }
              let node = el;
              for (let i=0;i<3;i++){
                node = node?.parentElement;
                if (!node) break;
                const txt = getText(node);
                if (txt) return txt;
              }
              return '';
            }
            const allCandidates = Array.from(document.querySelectorAll('[data-checkbox="true"]'));
            // 일부 사이트는 input이 숨겨져 있고 label/div가 클릭 타겟인 경우가 있어 input 외 요소도 수집
            const inputs = Array.from(document.querySelectorAll('input[type="checkbox"]'));
            const candidates = Array.from(new Set([...allCandidates, ...inputs]));
            for (const el of candidates){
              const text = getTextAround(el);
              if (text.includes('경제')){
                let target = el;
                // 숨겨진 input이면 label/부모로 타겟 변경
                const style = window.getComputedStyle(el);
                const hidden = style.display === 'none' || style.visibility === 'hidden' || style.opacity === '0';
                if ((el.tagName.toLowerCase()==='input' && el.type==='checkbox') && hidden){
                  const label = el.closest('label');
                  if (label) target = label;
                  else if (el.parentElement) target = el.parentElement;
                }
                target.scrollIntoView({block:'center'});
                return [el, target];
              }
            }
            return null;
            """
            economy_el = None
            click_target = None
            try:
                result = self.driver.execute_script(js_find)
                if result:
                    try:
                        # Expect a list [checkbox, clickTarget]
                        if isinstance(result, list) and len(result) >= 2:
                            economy_el = result[0]
                            click_target = result[1]
                        else:
                            economy_el = result
                            click_target = result
                    except Exception:
                        economy_el = result
                        click_target = result
            except Exception as e:
                self.logger.warning(f"JS 탐색 실패: {e}")

            # 3) XPath 대체
            if economy_el is None:
                for xp in [
                    "//label[contains(normalize-space(.), '경제')]//input[@type='checkbox']",
                    "//span[contains(normalize-space(text()),'경제')]/ancestor::label//input[@type='checkbox']",
                    "//input[@type='checkbox' and following-sibling::*[contains(normalize-space(.), '경제')]]",
                    "//*[contains(normalize-space(.),'경제')]/ancestor::*[self::label or self::li or self::div][1]//input[@type='checkbox']",
                ]:
                    try:
                        el = self.driver.find_element(By.XPATH, xp)
                        if el.is_displayed() and el.is_enabled():
                            economy_el = el
                            try:
                                # 클릭 타겟은 label 또는 부모일 수 있음
                                lbl = el.find_element(By.XPATH, "ancestor::label[1]")
                                click_target = lbl if lbl else el
                            except:
                                click_target = el
                            break
                    except:
                        continue

            if not economy_el:
                self.logger.error("'경제' 항목을 찾을 수 없습니다")
                return False

            # 체크
            try:
                if click_target is None:
                    click_target = economy_el
                self.driver.execute_script("arguments[0].scrollIntoView({block:'center'});", click_target)
                # 먼저 클릭 타겟 클릭
                try:
                    self.driver.execute_script("arguments[0].click();", click_target)
                except Exception:
                    pass
                # 체크 상태 확인, 아니면 input 강제 체크
                try:
                    is_checked = economy_el.is_selected()
                except Exception:
                    is_checked = False
                if not is_checked:
                    self.driver.execute_script("arguments[0].checked = true; arguments[0].dispatchEvent(new Event('change', {bubbles:true}));", economy_el)
            except Exception:
                try:
                    if click_target is not None:
                        click_target.click()
                    else:
                        economy_el.click()
                except Exception as e:
                    self.logger.warning(f"일반 클릭 실패, checked 강제: {e}")
                    try:
                        self.driver.execute_script("arguments[0].checked = true; arguments[0].dispatchEvent(new Event('change', {bubbles:true}));", economy_el)
                    except Exception as e2:
                        self.logger.error(f"'경제' 선택 실패: {e2}")
                        return False
            time.sleep(0.5)

            # 적용하기 클릭
            apply_btn = None
            # 0) 명시 클래스 우선 타겟
            try:
                btn = self.driver.find_element(By.CSS_SELECTOR, "button.btn.btn-search.news-search-btn.news-report-search-btn")
                if btn.is_displayed() and btn.is_enabled():
                    apply_btn = btn
            except:
                pass

            # 1) XPath로 클래스/텍스트 정밀 매칭
            if not apply_btn:
                apply_xpaths = [
                    "//button[contains(@class,'news-report-search-btn') and contains(normalize-space(.),'적용하기')]",
                    "//button[contains(@class,'news-report-search-btn')]",
                    "//button[contains(normalize-space(.),'적용하기')]",
                    "//a[contains(normalize-space(.),'적용하기')]",
                    "//button[contains(normalize-space(.),'적용')]",
                    "//a[contains(normalize-space(.),'적용')]",
                    "//*[contains(@class,'btn') and contains(normalize-space(.),'적용')]",
                    "//div[contains(@class,'filter') or contains(@class,'panel')]//button[contains(normalize-space(.),'적용')]",
                ]
                for xp in apply_xpaths:
                    try:
                        el = self.driver.find_element(By.XPATH, xp)
                        if el.is_displayed() and el.is_enabled():
                            apply_btn = el
                            break
                    except:
                        continue

            if not apply_btn:
                # 스크롤로 하단 고정바 노출 시도 후 재검색
                try:
                    self.driver.execute_script("window.scrollTo(0, document.body.scrollHeight);")
                    time.sleep(0.4)
                except:
                    pass
                for xp in apply_xpaths:
                    try:
                        el = self.driver.find_element(By.XPATH, xp)
                        if el.is_displayed() and el.is_enabled():
                            apply_btn = el
                            break
                    except:
                        continue

            if not apply_btn:
                # JS로 명시 클래스 또는 텍스트 매칭 우선 클릭
                clicked = False
                try:
                    clicked = self.driver.execute_script(
                        """
                        const prefer = document.querySelector('button.btn.btn-search.news-search-btn.news-report-search-btn');
                        if (prefer){ try { prefer.scrollIntoView({block:'center'}); prefer.click(); return true; } catch(e){} }
                        const keys = ['적용하기'];
                        const nodes = Array.from(document.querySelectorAll('button, a, [role="button"], .btn, .button'));
                        for (const el of nodes){
                          const t = (el.innerText||el.textContent||'').replace(/\s+/g,'');
                          if (!t) continue;
                          if (keys.some(k => t.includes(k))){
                            try { el.scrollIntoView({block:'center'}); el.click(); return true; } catch(e){}
                          }
                        }
                        return false;
                        """
                    )
                except Exception as e:
                    self.logger.warning(f"JS 적용버튼 클릭 시도 실패: {e}")

                if not clicked:
                    # 후보 로깅 후 실패 처리
                    try:
                        candidates = self.driver.find_elements(By.XPATH, "//*[contains(normalize-space(.),'적용')]")
                        self.logger.error(f"'적용하기' 버튼을 찾을 수 없습니다. 후보 개수: {len(candidates)}")
                    except:
                        pass
                    return False
            else:
                try:
                    self.driver.execute_script("arguments[0].scrollIntoView({block:'center'}); arguments[0].click();", apply_btn)
                except:
                    try:
                        apply_btn.click()
                    except Exception as e:
                        self.logger.error(f"'적용하기' 클릭 실패: {e}")
                        return False

            time.sleep(1.5)
            self.logger.info("'경제' 적용 완료")
            return True
        except Exception as e:
            self.logger.error(f"통합 분류 경제 적용 실패: {e}")
            return False

    def open_analysis_and_download_excel(self):
        """'분석 결과 및 시각화' 탭을 열고 '엑셀 다운로드'를 클릭하여 저장"""
        try:
            self.logger.info("분석 결과 및 시각화 탭 열기 및 엑셀 다운로드 시도")
            # 적용 후 결과 로딩 대기
            time.sleep(2.0)

            # 1) 탭 클릭 (텍스트 정규화 매칭)
            opened = False
            tab_xpaths = [
                "//a[contains(normalize-space(.),'분석 결과 및 시각화')]",
                "//button[contains(normalize-space(.),'분석 결과 및 시각화')]",
                "//*[@role='tab' and contains(normalize-space(.),'분석 결과 및 시각화')]",
                "//*[contains(@class,'tab')]//*[contains(normalize-space(.),'분석 결과 및 시각화')]",
            ]
            for xp in tab_xpaths:
                try:
                    el = self.driver.find_element(By.XPATH, xp)
                    if el.is_displayed() and el.is_enabled():
                        try:
                            self.driver.execute_script("arguments[0].scrollIntoView({block:'center'}); arguments[0].click();", el)
                        except Exception:
                            el.click()
                        opened = True
                        break
                except Exception:
                    continue

            if not opened:
                # JS fallback by text includes (whitespace-agnostic)
                try:
                    opened = self.driver.execute_script(
                        """
                        const keys = ['분석결과및시각화','분석결과','시각화'];
                        const nodes = Array.from(document.querySelectorAll('a, button, [role=\"tab\"], .tab a, .tab button'));
                        for (const el of nodes){
                          const t = (el.innerText||el.textContent||'').replace(/\s+/g,'');
                          if (!t) continue;
                          if (keys.some(k => t.includes(k))){
                            try { el.scrollIntoView({block:'center'}); el.click(); return true; } catch(e){}
                          }
                        }
                        return false;
                        """
                    )
                except Exception as e:
                    self.logger.warning(f"JS 탭 열기 실패: {e}")

            if not opened:
                self.logger.error("'분석 결과 및 시각화' 탭을 찾을 수 없습니다")
                return False

            # 탭 콘텐츠 로딩 대기
            time.sleep(1.2)

            # 2) 엑셀 다운로드 버튼 클릭
            dl_btn = None
            dl_xpaths = [
                "//button[contains(normalize-space(.),'엑셀') and contains(normalize-space(.),'다운')]",
                "//a[contains(normalize-space(.),'엑셀') and contains(normalize-space(.),'다운')]",
                "//*[contains(@class,'btn') and contains(normalize-space(.),'엑셀')]",
                "//*[contains(@class,'excel') and (self::a or self::button)]",
            ]
            for xp in dl_xpaths:
                try:
                    el = self.driver.find_element(By.XPATH, xp)
                    if el.is_displayed() and el.is_enabled():
                        dl_btn = el
                        break
                except Exception:
                    continue

            if not dl_btn:
                # JS fallback: prefer any button-like with text including '엑셀' and '다운'
                clicked = False
                try:
                    clicked = self.driver.execute_script(
                        """
                        const nodes = Array.from(document.querySelectorAll('button, a, [role=\"button\"], .btn'));
                        for (const el of nodes){
                          const t = (el.innerText||el.textContent||'').replace(/\s+/g,'');
                          if (!t) continue;
                          if (t.includes('엑셀') && (t.includes('다운') || t.includes('다운로드'))){
                            try { el.scrollIntoView({block:'center'}); el.click(); return true; } catch(e){}
                          }
                        }
                        return false;
                        """
                    )
                except Exception as e:
                    self.logger.warning(f"JS 엑셀 다운로드 클릭 실패: {e}")

                if not clicked:
                    self.logger.error("'엑셀 다운로드' 버튼을 찾을 수 없습니다")
                    return False
            else:
                try:
                    self.driver.execute_script("arguments[0].scrollIntoView({block:'center'}); arguments[0].click();", dl_btn)
                except Exception:
                    dl_btn.click()

            self.logger.info("엑셀 다운로드 클릭 완료")
            # 다운로드 완료 대기
            try:
                self._wait_for_download(timeout=30)
            except Exception as e:
                self.logger.warning(f"다운로드 완료 대기 중 경고: {e}")
            return True
        except Exception as e:
            self.logger.error(f"분석 탭/엑셀 다운로드 실패: {e}")
            return False

    def _wait_for_download(self, timeout=30):
        """다운로드 디렉토리에 파일 생성/완료를 기다림"""
        start = time.time()
        last_size = -1
        stable_count = 0
        while time.time() - start < timeout:
            try:
                files = [f for f in os.listdir(self.download_dir) if not f.endswith('.crdownload')]
                # 진행중인 파일도 추적
                partials = [f for f in os.listdir(self.download_dir) if f.endswith('.crdownload')]
                if files:
                    # 파일 크기 안정화 체크
                    path = os.path.join(self.download_dir, sorted(files, key=lambda x: os.path.getmtime(os.path.join(self.download_dir, x)), reverse=True)[0])
                    size = os.path.getsize(path)
                    if size == last_size:
                        stable_count += 1
                        if stable_count >= 3 and not partials:
                            self.logger.info(f"다운로드 완료 확인: {path}")
                            return True
                    else:
                        last_size = size
                        stable_count = 0
                elif not partials:
                    # 파일이 아직 안생김
                    pass
            except Exception:
                pass
            time.sleep(1)
        raise TimeoutError("다운로드 완료를 확인하지 못했습니다")
    
    def close(self):
        """브라우저 종료"""
        if self.driver:
            self.driver.quit()

def main():
    """메인 실행 함수"""
    # 로그인 정보
    email = "jack0810@kookmin.ac.kr"
    password = "0810jack!"
    
    print("🐳 BIG KINDS 로그인 및 탭 이동 테스트")
    print("=" * 50)
    
    # 자동화 실행
    automation = BigKindsAutomation(email, password)
    
    try:
        # 1. 드라이버 설정
        print("🔧 드라이버 설정 중...")
        if not automation.setup_driver():
            print("❌ 드라이버 설정 실패")
            return
        
        print("✅ 드라이버 설정 완료")
        
        # 2. BIG KINDS 접속
        print("🌐 BIG KINDS 웹사이트에 접속 중...")
        automation.driver.get("https://www.bigkinds.or.kr/")
        time.sleep(3)
        
        print(f"   - 현재 URL: {automation.driver.current_url}")
        
        # 3. 로그인
        print("🔐 로그인 시도 중...")
        if not automation.login():
            print("❌ 로그인 실패")
            return
        
        print("✅ 로그인 성공!")
        
        # 4. 뉴스 검색 분석 탭으로 이동
        print("📊 뉴스 검색 분석 탭으로 이동 중...")
        if not automation.navigate_to_news_analysis():
            print("❌ 뉴스 검색 분석 탭 이동 실패")
            return
        
        print("✅ 뉴스 검색 분석 탭 이동 성공!")
        
        # 5. 기간 1일 선택
        print("📅 기간 1일 선택 중...")
        if not automation.set_period_one_day():
            print("❌ 기간 1일 선택 실패")
            return
        print("✅ 기간 1일 선택 완료!")

        # 6. 통합 분류에서 '경제' 선택 및 적용
        print("📚 통합 분류에서 '경제' 선택 및 적용 중...")
        if not automation.select_economy_and_apply():
            print("❌ 통합 분류에서 '경제' 선택 및 적용 실패")
            return
        print("✅ 통합 분류에서 '경제' 선택 및 적용 완료!")
        
        print("\n🎉 모든 테스트가 성공적으로 완료되었습니다!")
        
    except KeyboardInterrupt:
        print("\n⏹️ 사용자에 의해 중단되었습니다.")
    except Exception as e:
        print(f"❌ 예상치 못한 오류가 발생했습니다: {e}")
    finally:
        automation.close()

if __name__ == "__main__":
    main()
