💱 Currency Converter REST Service

![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen?logo=springboot)
![Maven](https://img.shields.io/badge/Maven-3.9-blue?logo=apachemaven)
![JUnit 5](https://img.shields.io/badge/JUnit-5-red?logo=junit5)
![License](https://img.shields.io/badge/license-MIT-lightgrey)

REST-сервис для конвертации валют, реализованный на стеке **Java 17 + Spring Boot 3.2 + Maven**.
Получает актуальные курсы из открытого API [`open.er-api.com`](https://open.er-api.com/),
кэширует их в памяти, сохраняет результаты конвертаций в базу данных H2 и подсчитывает
количество обращений потокобезопасным счётчиком.

---

## 📋 Содержание

- [Возможности](#-возможности)
- [Стек технологий](#-стек-технологий)
- [Архитектура](#-архитектура)
- [Быстрый старт](#-быстрый-старт)
- [REST API](#-rest-api)
- [Примеры запросов](#-примеры-запросов)
- [Тестирование](#-тестирование)
- [Нагрузочное тестирование](#-нагрузочное-тестирование)
- [Структура проекта](#-структура-проекта)

---

## ✨ Возможности

- 🔄 **Конвертация валют** — одиночная и пакетная (bulk)
- 🌐 **Актуальные курсы** — получение из открытого API `open.er-api.com`
- ⚡ **In-memory кэш** — `ConcurrentHashMap` в отдельном Spring-бине через DI
- 🧮 **Потокобезопасный счётчик** — синхронизированный подсчёт обращений
- 📦 **Bulk-операции** — конвертация списка сумм через Java 8 Stream API
- 💾 **Персистентность** — сохранение результатов в H2 (Spring Data JPA)
- 🛡️ **Валидация и обработка ошибок** — 400 / 500 через `@RestControllerAdvice`
- 📝 **Логирование** — SLF4J + Logback, раздельные уровни INFO / DEBUG / ERROR
- ✅ **Unit-тесты** — JUnit 5 + Mockito, покрытие ~94%

---

## 🛠 Стек технологий

| Технология | Версия | Назначение |
|---|---|---|
| Java | 17 | Язык программирования |
| Spring Boot | 3.2.0 | Основной фреймворк |
| Spring Web | 6.x | REST-контроллеры |
| Spring Data JPA | 3.x | Работа с БД |
| Hibernate | 6.x | ORM |
| H2 Database | 2.x | Встроенная СУБД |
| Jackson | 2.x | JSON-сериализация |
| JUnit 5 | 5.10 | Модульные тесты |
| Mockito | 5.x | Mock-объекты |
| Maven | 3.9 | Сборка проекта |

---

## 🏗 Архитектура

```
┌─────────────────────────────────────────────────────────┐
│                     REST API Layer                       │
│                  CurrencyController                      │
│   GET  /api/currency/convert                             │
│   POST /api/currency/convert/bulk                        │
│   GET  /api/currency/stats                               │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│                    Service Layer                         │
│                  CurrencyService                         │
│   • convert()        • convertBulk()                     │
│   • getRate()        • validate()                        │
└────┬──────────────┬───────────────────┬─────────────────┘
     │              │                   │
┌────▼────┐  ┌──────▼──────┐  ┌─────────▼────────┐
│  Cache  │  │  Counter    │  │   Repository     │
│Currency │  │  Request    │  │   Conversion     │
│  Cache  │  │  Counter    │  │   Result         │
│(Map)    │  │(synchronized)│ │(Spring Data JPA) │
└─────────┘  └─────────────┘  └─────────┬────────┘
                                        │
                                 ┌──────▼──────┐
                                 │   H2 DB     │
                                 │conversion_  │
                                 │results      │
                                 └─────────────┘
```

---

## 🚀 Быстрый старт

### Требования

- **JDK 17** или выше
- **Maven 3.8+** (или используйте Maven Wrapper `mvnw`)
- **IntelliJ IDEA** (рекомендуется)

### Клонирование и запуск

```bash
git clone https://github.com/ВАШ_ЛОГИН/currency-converter.git
cd currency-converter
mvn spring-boot:run
```

Приложение запустится на **http://localhost:8080**.

### Альтернатива — через IntelliJ IDEA

1. **File → Open** → выберите папку проекта
2. Дождитесь загрузки Maven-зависимостей
3. Запустите `CurrencyConverterApplication` (зелёная ▶)

---

## 🌐 REST API

### Базовый URL

```
http://localhost:8080
```

### Эндпоинты

| Метод | Путь | Описание |
|---|---|---|
| `GET` | `/api/currency/convert` | Конвертация одной суммы |
| `POST` | `/api/currency/convert/bulk` | Массовая конвертация списка сумм |
| `GET` | `/api/currency/stats` | Количество обращений к сервису |
| `GET` | `/h2-console` | Веб-консоль H2 |

---

## 📬 Примеры запросов

### 1. Конвертация одной суммы

**Запрос:**

```bash
curl "http://localhost:8080/api/currency/convert?amount=100&from=USD&to=EUR"
```

**Ответ (200 OK):**

```json
{
  "amount": 100.0,
  "fromCurrency": "USD",
  "toCurrency": "EUR",
  "result": 92.0
}
```

### 2. Ошибка — невалидная сумма

```bash
curl "http://localhost:8080/api/currency/convert?amount=-5&from=USD&to=EUR"
```

**Ответ (400 Bad Request):**

```json
{
  "error": "Amount must be positive"
}
```

### 3. Ошибка — несуществующая валюта

```bash
curl "http://localhost:8080/api/currency/convert?amount=100&from=UUU&to=EUR"
```

**Ответ (400 Bad Request):**

```json
{
  "error": "Unsupported currency code: UUU"
}
```

### 4. Bulk-конвертация

**Запрос:**

```bash
curl -X POST "http://localhost:8080/api/currency/convert/bulk" \
  -H "Content-Type: application/json" \
  -d '{
    "amounts": [10, 20, 30],
    "fromCurrency": "USD",
    "toCurrency": "EUR"
  }'
```

**Ответ (200 OK):**

```json
{
  "results": [
    { "amount": 10.0, "fromCurrency": "USD", "toCurrency": "EUR", "result": 9.2 },
    { "amount": 20.0, "fromCurrency": "USD", "toCurrency": "EUR", "result": 18.4 },
    { "amount": 30.0, "fromCurrency": "USD", "toCurrency": "EUR", "result": 27.6 }
  ]
}
```

### 5. Статистика обращений

```bash
curl "http://localhost:8080/api/currency/stats"
```

**Ответ (200 OK):**

```json
{
  "requests": 42
}
```

### 6. Просмотр БД

Откройте в браузере: **http://localhost:8080/h2-console**

| Параметр | Значение |
|---|---|
| JDBC URL | `jdbc:h2:file:./data/currencydb` |
| User Name | `sa` |
| Password | (пусто) |

```sql
SELECT * FROM CONVERSION_RESULTS ORDER BY TIMESTAMP DESC;
```

---

## 🧪 Тестирование

### Запуск тестов

```bash
mvn test
```

### Запуск с отчётом покрытия

```bash
mvn clean test
```

HTML-отчёт откроется в `target/site/jacoco/index.html`.

### Покрытие

| Метрика | Значение |
|---|---|
| Class Coverage | 83% |
| Method Coverage | 94% |
| Line Coverage | 94% |
| Branch Coverage | 80% |

### Что покрыто

- ✅ Валидация входных параметров (`amount`, `from`, `to`)
- ✅ Успешная конвертация (с замоканным `RestTemplate`)
- ✅ Обработка ошибок внешнего API
- ✅ Работа кэша (попадание, промах, очистка)
- ✅ Сохранение результатов в БД
- ✅ Потокобезопасность счётчика (многопоточный тест)
- ✅ Bulk-операции и Stream API
- ✅ HTTP-слой: коды 200 / 400 / 500

---

## 🔥 Нагрузочное тестирование

### Postman Collection Runner

1. Создайте коллекцию с запросом:

```
GET http://localhost:8080/api/currency/convert?amount=100&from=USD&to=EUR
```

2. **Runner** → Iterations = `1000`, Delay = `0`
3. Запустите
4. Проверьте счётчик:

```
GET http://localhost:8080/api/currency/stats
```

Ожидаемый результат: `{"requests": 1000}`

### Apache JMeter

1. **Thread Group:** Threads = 50, Loop Count = 20 (итого 1000 запросов)
2. **HTTP Request:** GET `/api/currency/convert?amount=100&from=USD&to=EUR`
3. **Listeners:** View Results Tree + Summary Report
4. Запуск → проверка счётчика через `/api/currency/stats`

---

## 📁 Структура проекта

```
currency-converter/
├── src/
│   ├── main/
│   │   ├── java/com/example/currencyconverter/
│   │   │   ├── CurrencyConverterApplication.java
│   │   │   ├── cache/
│   │   │   │   └── CurrencyCache.java
│   │   │   ├── config/
│   │   │   │   └── RestTemplateConfig.java
│   │   │   ├── controller/
│   │   │   │   └── CurrencyController.java
│   │   │   ├── dto/
│   │   │   │   ├── BulkConversionRequest.java
│   │   │   │   ├── BulkConversionResponse.java
│   │   │   │   └── ConversionResponse.java
│   │   │   ├── entity/
│   │   │   │   └── ConversionResult.java
│   │   │   ├── exception/
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   └── InvalidParameterException.java
│   │   │   ├── repository/
│   │   │   │   └── ConversionResultRepository.java
│   │   │   └── service/
│   │   │       ├── CurrencyService.java
│   │   │       └── RequestCounterService.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/com/example/currencyconverter/
│           ├── CurrencyServiceTest.java
│           └── CurrencyControllerTest.java
├── .gitignore
├── pom.xml
└── README.md
```

---