module kkoishi.json {
    exports top.kkoishi.json;
    exports top.kkoishi.json.io;
    exports top.kkoishi.json.parse;
    exports top.kkoishi.json.reflect;
    exports top.kkoishi.json.annotation;
    exports top.kkoishi.json.exceptions;

    requires java.sql;
    requires static jdk.unsupported;
    requires kotlin.stdlib;
    requires kotlin.stdlib.jdk7;
    requires kotlin.stdlib.jdk8;

    opens top.kkoishi.json;
    opens top.kkoishi.json.io;
    opens top.kkoishi.json.parse;
    opens top.kkoishi.json.reflect;
    opens top.kkoishi.json.annotation;
    opens top.kkoishi.json.exceptions;
}