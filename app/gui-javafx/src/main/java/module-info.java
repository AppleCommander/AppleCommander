module org.applecommander.javafx {
    requires atlantafx.base;
    requires java.prefs;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;
    requires org.applecommander.api;
    requires org.applecommander.applesingle;
    requires org.applecommander.bastools;
    requires org.applecommander.shrinkit;
    requires org.applecommander.disassembler;
    requires org.fxmisc.richtext;
    requires com.google.common;
    requires org.fxmisc.flowless;

    opens org.applecommander.javafx to javafx.graphics;
    opens org.applecommander.javafx.settings to javafx.graphics;
    opens org.applecommander.javafx.wizard to javafx.graphics;
    opens org.applecommander.javafx.filter to javafx.graphics;
    opens org.applecommander.javafx.filestore to javafx.graphics;
    opens org.applecommander.javafx.filestore.view to javafx.graphics;
    opens org.applecommander.javafx.content to javafx.graphics;
}