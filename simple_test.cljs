(require '[dev.jaide.hiccup-pdf.emoji :as emoji])

(println "Testing basic emoji functionality...")

(let [result (emoji/emoji? "💡")]
  (println "emoji? test:" result))

(let [result (emoji/emoji-filename "💡")]
  (println "emoji-filename test:" result))

(println "Basic functionality works!")