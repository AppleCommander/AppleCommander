module org.applecommander.javafx {
    requires atlantafx.base;
    requires java.prefs;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;
    requires org.applecommander.api;
    requires org.applecommander.applesingle;

    opens org.applecommander.javafx to javafx.graphics;
    opens org.applecommander.javafx.settings to javafx.graphics;
    opens org.applecommander.javafx.wizard to javafx.graphics;
}