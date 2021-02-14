# Dynamic Array
В этом задании вам необходимо реализовать lock-free динамически расширяемый массив, используя
идеи из лекции про хеш-таблицу. В данном случае диаграмма состояний получается куда проще, 
а перенос элементов остается таким же. Подумайте над тем, как сделать операции `pushBack` и `get` 
атомарными -- возможно, идейно поможет очередь Майкла-Скотта.

Реализовывать по-настоящему кооперативный перенос элементов в новый массив необязательно, 
хотя это и очень важно на практике. Для выполнения данного задания допустима реализация, 
в которой перенос начинают и заканчивают те потоки, которые натолкнулись на необходимость 
его делать.

В файле [`src/DynamicArray.kt`](src/DynamicArray.kt) находится описание интерфейса массива, 
который  вам необходимо реализовать. Ваше решение должно быть в файле [`src/DynamicArrayImpl.kt`](src/DynamicArrayImpl.kt).


Для проверки запустите из корня репозитория:
* `./gradlew test` на Linux или MacOS
* `.\gradlew.bat test` на Windows