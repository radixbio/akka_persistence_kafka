package(
    default_visibility = ["//shared/persistence:__subpackages__"],
)

scala_library(
    name = "squants-serializations",
    srcs = [
        "schemas.scala",
        "units.scala",
    ],
    exports = [
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
        "@third_party//3rdparty/jvm/org/typelevel:squants",
    ],
    deps = [
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
        "@third_party//3rdparty/jvm/org/typelevel:squants",
    ],
)
