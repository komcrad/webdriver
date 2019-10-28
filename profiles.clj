{:user {:dependencies [[komcrad/repl-reload "0.1.0"]]
        :plugins [[lein-cloverage "1.1.2"]
                  [cider/cider-nrepl "0.22.4"]]
        :repl-options
        {:init (do (require 'clojure.tools.namespace.repl 'repl-reload.core)
                   (clojure.tools.namespace.repl/refresh)
                   (repl-reload.core/auto-reload)
                   (use 'clojure.repl))}}}
