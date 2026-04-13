module com.chatproject {
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.base;
    requires jakarta.persistence;
    requires org.hibernate.orm.core;

    opens Client to javafx.graphics, javafx.base;
    exports Client;
    exports DBUtil;
    exports Server;
    exports Util;
}
