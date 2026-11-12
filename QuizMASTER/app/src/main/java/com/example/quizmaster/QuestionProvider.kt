package com.example.quizmaster

object QuestionProvider {
    fun getQuestions(category: String): List<Question> {
        return when (category) {
            "Python" -> pythonQuestions()
            "Java" -> javaQuestions()
            "Android" -> androidQuestions()
            "SQL" -> sqlQuestions()
            else -> generalQuestions()
        }
    }

    private fun pythonQuestions() = listOf(
        Question("py_1", "Which keyword is used to define a function in Python?", "function", "def", "fun", "define", 2),
        Question("py_2", "Which data type is used to store text?", "int", "float", "str", "bool", 3),
        Question("py_3", "Which symbol is used for comments in Python?", "//", "#", "/*", "<!--", 2),
        Question("py_4", "Which function is used to display output?", "display()", "show()", "print()", "output()", 3),
        Question("py_5", "Which collection is ordered and changeable?", "Tuple", "List", "Set", "FrozenSet", 2),
        Question("py_6", "Which keyword is used for a loop?", "repeat", "loop", "for", "iterate", 3),
        Question("py_7", "What is the extension of a Python file?", ".java", ".py", ".kt", ".python", 2),
        Question("py_8", "Which library is commonly used for data analysis?", "Pandas", "React", "Spring", "Laravel", 1),
        Question("py_9", "Which keyword creates a class?", "object", "class", "struct", "new", 2),
        Question("py_10", "Which operator is used for exponentiation?", "^", "**", "//", "%%", 2)
    )

    private fun javaQuestions() = listOf(
        Question("ja_1", "Which keyword is used to create a class?", "class", "Class", "new", "object", 1),
        Question("ja_2", "Which method is the entry point of a Java program?", "start()", "main()", "run()", "execute()", 2),
        Question("ja_3", "Which keyword is used to inherit a class?", "implements", "inherits", "extends", "super", 3),
        Question("ja_4", "Which data type stores whole numbers?", "float", "int", "double", "char", 2),
        Question("ja_5", "Which keyword creates an object?", "create", "object", "new", "make", 3),
        Question("ja_6", "Which concept allows method overloading?", "Polymorphism", "Inheritance", "Encapsulation", "Abstraction", 1),
        Question("ja_7", "Which symbol ends a Java statement?", ".", ":", ";", ",", 3),
        Question("ja_8", "Java was originally developed by?", "Google", "Sun Microsystems", "Microsoft", "Apple", 2),
        Question("ja_9", "Which keyword prevents inheritance?", "static", "final", "private", "const", 2),
        Question("ja_10", "Which package contains ArrayList?", "java.io", "java.util", "java.lang", "java.sql", 2)
    )

    private fun androidQuestions() = listOf(
        Question("an_1", "Which IDE is used for Android development?", "Eclipse", "Android Studio", "Visual Studio", "NetBeans", 2),
        Question("an_2", "Which file contains Android permissions?", "MainActivity.kt", "build.gradle", "AndroidManifest.xml", "settings.xml", 3),
        Question("an_3", "Which method is called when Activity starts?", "onCreate()", "onStartApp()", "startActivity()", "begin()", 1),
        Question("an_4", "Which language is officially recommended for Android?", "Kotlin", "PHP", "Python", "Ruby", 1),
        Question("an_5", "Which component displays a list?", "TextView", "RecyclerView", "ImageView", "EditText", 2),
        Question("an_6", "What is XML mainly used for in Android?", "Database", "UI layout", "Networking", "Authentication", 2),
        Question("an_7", "Which class is used for screen navigation?", "Intent", "Navigate", "Screen", "Route", 1),
        Question("an_8", "Which file contains app-level dependencies?", "AndroidManifest.xml", "build.gradle.kts", "MainActivity.kt", "strings.xml", 2),
        Question("an_9", "Which component is used for user text input?", "TextView", "EditText", "ImageView", "Button", 2),
        Question("an_10", "What does APK stand for?", "Android Package Kit", "Android Program Key", "Application Package Kotlin", "Android Project Kit", 1)
    )

    private fun sqlQuestions() = listOf(
        Question("sq_1", "Which command is used to retrieve data?", "GET", "SELECT", "FETCH", "READ", 2),
        Question("sq_2", "Which command adds new data?", "ADD", "INSERT", "CREATE", "PUT", 2),
        Question("sq_3", "Which command modifies existing data?", "CHANGE", "UPDATE", "MODIFY", "ALTER", 2),
        Question("sq_4", "Which command removes records?", "REMOVE", "DELETE", "DROP", "CLEAR", 2),
        Question("sq_5", "Which clause filters rows?", "FILTER", "WHERE", "HAVING", "CHECK", 2),
        Question("sq_6", "Which clause sorts results?", "SORT BY", "ORDER BY", "GROUP BY", "ARRANGE", 2),
        Question("sq_7", "Which function counts rows?", "TOTAL()", "COUNT()", "NUMBER()", "ROWS()", 2),
        Question("sq_8", "Which key uniquely identifies a record?", "Foreign Key", "Primary Key", "Candidate Key", "Secondary Key", 2),
        Question("sq_9", "Which command creates a table?", "MAKE TABLE", "CREATE TABLE", "NEW TABLE", "BUILD TABLE", 2),
        Question("sq_10", "Which operator is used for pattern matching?", "MATCH", "LIKE", "SEARCH", "PATTERN", 2)
    )

    private fun generalQuestions() = listOf(
        Question("gn_1", "What is the capital of India?", "Mumbai", "New Delhi", "Kolkata", "Chennai", 2),
        Question("gn_2", "How many days are there in a week?", "5", "6", "7", "8", 3),
        Question("gn_3", "Which planet is known as the Red Planet?", "Earth", "Mars", "Jupiter", "Venus", 2),
        Question("gn_4", "How many continents are there?", "5", "6", "7", "8", 3),
        Question("gn_5", "Which is the largest ocean?", "Atlantic Ocean", "Indian Ocean", "Pacific Ocean", "Arctic Ocean", 3),
        Question("gn_6", "What is the national animal of India?", "Lion", "Tiger", "Elephant", "Peacock", 2),
        Question("gn_7", "Which gas do humans need to breathe?", "Carbon Dioxide", "Oxygen", "Nitrogen", "Hydrogen", 2),
        Question("gn_8", "How many hours are there in a day?", "12", "18", "24", "48", 3),
        Question("gn_9", "Which is the fastest land animal?", "Lion", "Cheetah", "Horse", "Tiger", 2),
        Question("gn_10", "Which is the largest planet?", "Earth", "Saturn", "Jupiter", "Neptune", 3)
    )
}
