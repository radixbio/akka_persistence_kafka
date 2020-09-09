scala_library(
    name = "dbpmjss-uservice-serializations",
    srcs = [
        "protocol.scala",
    ],
    resources = ["resources/reference.conf"],
    visibility = [
        "//algs/dbpmjss-uservice/jvm:__subpackages__",
        "//shared/persistence:__subpackages__",
    ],
    exports = [
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/algs/dbpmjss:dbpmjss-serializations",
    ],
    deps = [
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/algs/dbpmjss:dbpmjss-serializations",
    ],
)
