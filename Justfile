set dotenv-load := true
project := "exring"


help:
  @just --list


# Run tests
test:
  @npx shadow-cljs compile test


# Make a release, creates a tag and pushes it
@release version +message:
  git tag -a {{ version }} -m "{{ message }}"
  git push --tags
  bash -c 'echo -n "SHA: "'
  git rev-parse --short {{ version }}^{commit}


# Check for outdated deps
outdated:
  clj -M:outdated


# Initialize dev setup:
init: clojure-setup npm-setup
  @echo "\n\nReady"


# Initialize Clojure
clojure-setup:
  clojure -A:dev:test:example -P


# Initialize NPM
npm-setup:
  npm i
