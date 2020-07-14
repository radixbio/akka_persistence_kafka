load(
    "@io_bazel_rules_scala//scala:scala.bzl",
    "scala_library",
)

package(
    default_visibility = ["//shared/persistence:__subpackages__"],
)

scala_library(
    name = "squants-serializations",
    srcs = [
        "schemas.scala",
    ],
    exports = [
        "@third_party//3rdparty/jvm/org/typelevel:squants",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
    ],
    deps = [
        "@third_party//3rdparty/jvm/org/typelevel:squants",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
    ],
)
