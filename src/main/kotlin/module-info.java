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
}