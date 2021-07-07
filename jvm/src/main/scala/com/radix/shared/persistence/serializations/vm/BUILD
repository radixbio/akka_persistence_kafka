package(
    default_visibility = ["//shared/persistence:__subpackages__"],
)

scala_library(
    name = "vm-serializations",
    srcs = [
        "defns.scala",
        "uservice/actorInfo.scala",
    ],
    resources = ["resources/reference.conf"],
    exports = [
    ],
    deps = [
        "//rainbow/jvm:rainbow-lab-lib",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/squants:squants-serializations",
        "@third_party//3rdparty/jvm/com/typesafe/akka:akka_actor_typed",
    ],
)
