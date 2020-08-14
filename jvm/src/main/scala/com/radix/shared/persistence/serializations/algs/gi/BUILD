package(
    default_visibility = ["//shared/persistence:__subpackages__"],
)

scala_library(
    name = "gi-serializations",
    srcs = [
        "GIGraph.scala",
    ],
    resources = ["resources/application.conf"],
    exports = [
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
    ],
    deps = [
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
    ],
)