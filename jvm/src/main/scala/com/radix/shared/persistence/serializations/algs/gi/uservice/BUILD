load(
    "@io_bazel_rules_scala//scala:scala.bzl",
    "scala_library",
)

package(
    default_visibility = ["//shared/persistence:__subpackages__"],
)

scala_library(
    name = "gi-uservice-serializations",
    srcs = [
        "protocol.scala",
    ],
    resources = ["application.conf"],
    exports = [
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/algs/gi:gi-serializations",
    ],
    deps = [
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/algs/gi:gi-serializations",
    ],
)
