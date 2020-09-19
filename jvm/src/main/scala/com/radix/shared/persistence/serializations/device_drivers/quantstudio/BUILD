package(
    default_visibility = ["//shared/persistence:__subpackages__"],
)

scala_library(
    name = "quantstudio-serializations",
    srcs = [
        "actorInfo.scala",
        "defns.scala",
    ],
    resources = ["resources/reference.conf"],
    deps = [
        "@third_party//3rdparty/jvm/com/typesafe/akka:akka_actor_typed",
    ],
)
