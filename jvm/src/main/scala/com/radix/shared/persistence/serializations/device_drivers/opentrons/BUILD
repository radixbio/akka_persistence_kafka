package(
    default_visibility = ["//shared/persistence:__subpackages__"],
)

scala_library(
    name = "opentrons-serializations",
    srcs = [
        "actorInfo.scala",
        "defns.scala",
    ],
    resources = ["resources/reference.conf"],
    exports = [
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/squants:squants-serializations",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/utils/prism:prism-serialization",
        "//utils/prism/shared:prism-shared",
        "@third_party//3rdparty/jvm/com/typesafe/akka:akka_actor_typed",
    ],
    deps = [
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/squants:squants-serializations",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/utils/prism:prism-serialization",
        "//utils/prism/shared:prism-shared",
        "@third_party//3rdparty/jvm/com/typesafe/akka:akka_actor_typed",
    ],
)
