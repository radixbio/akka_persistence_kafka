scala_library(
    name = "dbpmjss-serializations",
    srcs = [
        "datatypes.scala",
    ],
    resources = ["resources/application.conf"],
    visibility = [
        "//algs/dbpmjss-uservice/jvm:__subpackages__",
        "//shared/persistence:__subpackages__",
    ],
    exports = [
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
    ],
    deps = [
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
    ],
)