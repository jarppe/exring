(ns user
  (:require [shadow.cljs.devtools.api :as shadow]))


(defn repl [build-id]
  (shadow/repl build-id))


#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn repl-web [] (repl :web))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn repl-node [] (repl :node))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn repl-api [] (repl :functions))


(comment

  (require 'match.impl.run)

  (shadow/compile :node)
  (shadow/watch :test)
  :cljs/quit
  (repl :web)
  (repl :node)
  (repl :functions)
  (repl :test)

  (shadow/active-builds)
  ;; => #{:functions :web}

  (shadow/repl :web)
  (js/console.log "Hello!")
  :cljs/quit

  (shadow/repl :functions)
  (js/console.log "Hello")
  :cljs/quit

  ;
  )

