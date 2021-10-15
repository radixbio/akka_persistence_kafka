package(
    default_visibility = ["//shared/persistence:__subpackages__"],
)

scala_library(
    name = "archetype-serializations",
    srcs = glob(["*.scala"]),
    resources = ["resources/reference.conf"],
    exports = [
        "//rainbow/jvm:urainbow-lib",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/squants:squants-serializations",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/utils/akka:akka-util-serializations",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/utils/rainbow:rainbow-serialization",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/vm:vm-serializations",
        "@third_party//3rdparty/jvm/org/typelevel:squants",
    ],
    deps = [
        "//rainbow/jvm:urainbow-lib",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/squants:squants-serializations",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/utils/akka:akka-util-serializations",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/utils/rainbow:rainbow-serialization",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/vm:vm-serializations",
        "@third_party//3rdparty/jvm/org/typelevel:squants",
    ],
)
