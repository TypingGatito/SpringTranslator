# Spring Translator

## Технологии
- Java 20
- Spring Data JPA
- JDBC
- PostgreSQL

## Запуск проекта
1. Склонируйте репозиторий:
    ```
    git clone https://github.com/TypingGatito/SpringTranslator
    ```

2. Настройте базу данных:
    - Создайте базу данных
    - Создайте в базе данных таблицу `translation_requests`, запустив файл createDB.sql
    - Обновите файл `application.properties` с вашими данными доступа к базе данных (jdbc.driverClassName, jdbc.url, jdbc.username, jdbc.password)
    - Разместите приложение на сервере


## Использование
1. Отправьте POST-запрос на `/rest_api/translate` с параметрами в JSON формате:
- `text`: строка для перевода
- `from`: исходный язык
- `to`: целевой язык

Ответ будет дан в JSON формате с ключом 'translation'

2. Отправьте POST-запрос на `/rest_api/translateT` с параметрами в текстовом формате:
from->to
text, где
- `text`: строка для перевода
- `from`: исходный язык
- `to`: целевой язык

Ответ будет дан в текстовом формате

3. Отправьте GET-запрос на `/rest_api/languages` 

Ответ будет дан в JSON формате с ключами сокра

4. Отправьте GET-запрос на корневой адрес сервера

Ответ будет дан в html формате