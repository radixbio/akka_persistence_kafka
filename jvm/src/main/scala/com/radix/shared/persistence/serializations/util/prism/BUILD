load(
    "@io_bazel_rules_scala//scala:scala.bzl",
    "scala_library",
)

package(
    default_visibility = ["//shared/persistence:__subpackages__"],
)

scala_library(
    name = "prism-serialization",
    srcs = [
        "derivations.scala",
    ],
    exports = [
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/squants:squants-serializations",
        "//utils/prism/shared:prism-shared",
    ],
    deps = [
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/squants:squants-serializations",
        "//utils/prism/shared:prism-shared",
    ],
)
