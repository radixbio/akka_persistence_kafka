package(
    default_visibility = ["//shared/persistence:__subpackages__"],
)


scala_library(
    name = "bioutil-serializations",
    srcs = ["defns.scala"],
    resources = ["resources/reference.conf"],
    exports = [],
    deps = [
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
        "@third_party//3rdparty/jvm/com/typesafe/akka:akka_actor_typed",
    ]
)