    Протестировал на API 19, API 25.
    
    Исправил все замечания анализаторов.
    Немного подправил конфиг checkstyle под Google code style + Android от себя.
    
    Исключил в тестах реальное обращение к серверу.
    За место этого написал мини-http-сервер, под потребности API, который запускается на девайсе при тестах.
    При запуске тестов, ссылка на сервер, автоматически, подменяется на http://localhost:8080.
    
    Сам сервер:
    \root\app\src\androidTest\java\com\gamaliev\notes\common\rest\NotesHttpServerTest.java
    
    Архитектура проекта: MVP.
    В данный момент протестировал все модели проекта.
    Осталось View и Presenter. 
    
    Не успеваю :( Но точно доведу до конца!
    
    
    ==== Code quality ====
    
    start:
    ./gradlew check -x test
    
    reports:
    \root\build\reports\
    
    
    ==== Jacoco ====
    
    start:
    ./gradlew uninstallAll
    ./gradlew jacocoTestReport
    
    androidTest report:
    \root\app\build\reports\androidTests\connected\index.html
    
    test report:
    \root\app\build\reports\tests\testDebugUnitTest\index.html
    
    jacoco report:
    \root\app\build\reports\jacoco\jacocoTestReport\html\index.html