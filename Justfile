set dotenv-load := true
project := "tech-talk"


help:
  @just --list


# Check for outdated deps
outdated:
  clojure -M:srv:web:test:outdated


# Run tests
test +opts=":unit --focus":
  @clear
  @echo "Running tests..."
  @clojure -M:test -m kaocha.runner         \
           --reporter kaocha.report/dots    \
           {{ opts }}


# Run ShadowCljs command
shadow cmd:
  npx shadow-cljs {{ cmd }}


# Initialize dev setup:
init: clojure-setup npm-setup
  @echo "\n\nReady"


# Clojure setup
clojure-setup:
  clojure -A:dev:test:example -P


# NPM setup
npm-setup:
  npm i
