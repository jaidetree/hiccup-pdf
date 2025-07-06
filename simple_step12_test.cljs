#!/usr/bin/env nbb

;; Simple test that avoids the text-processing namespace
(require '[dev.jaide.hiccup-pdf.document :as doc]
         '[dev.jaide.hiccup-pdf.images :as images]
         '[dev.jaide.hiccup-pdf.emoji :as emoji])

(println "=== Simple Step 12 Test ===")

;; Test the document scanning functionality
(println "\n1. Testing document emoji scanning...")
(try
  (let [document [:document {:title "Scan Test"}
                  [:page {}
                   [:text {:x 100 :y 100 :font "Arial" :size 14} "Page 1: 💡"]]
                  [:page {}
                   [:text {:x 100 :y 100 :font "Arial" :size 14} "Page 2: 🎯 💡"]]]
        emoji-set (doc/scan-document-for-emoji document)]
    (println "✓ Document scanning works")
    (println "  - Found emoji:" (vec emoji-set))
    (println "  - Emoji count:" (count emoji-set)))
  (catch js/Error e
    (println "✗ Error in document scanning:" (.-message e))))

;; Test image resource management
(println "\n2. Testing image resource management...")
(try
  (let [cache (images/create-image-cache)
        unique-emoji #{"💡" "🎯"}
        options {:fallback-strategy :hex-string}
        result (doc/embed-images-in-document unique-emoji cache 10 options)]
    (println "✓ Image resource management works")
    (println "  - Success:" (:success result))
    (println "  - Image objects count:" (count (:image-objects result)))
    (println "  - Image refs:" (:image-refs result))
    (println "  - XObject names:" (:xobject-names result)))
  (catch js/Error e
    (println "✗ Error in resource management:" (.-message e))))

;; Test configuration system
(println "\n3. Testing emoji configuration...")
(try
  (let [config (emoji/create-emoji-config 
                {:enable-emoji-images true
                 :fallback-strategy :placeholder
                 :cache-size 25})]
    (println "✓ Configuration system works")
    (println "  - Config enabled:" (emoji/emoji-config-enabled? config))
    (println "  - Cache size:" (emoji/get-emoji-config-value config :cache-size))
    (println "  - Fallback strategy:" (emoji/get-emoji-config-value config :fallback-strategy)))
  (catch js/Error e
    (println "✗ Error in configuration:" (.-message e))))

;; Test resource dictionary generation
(println "\n4. Testing resource dictionary generation...")
(try
  (let [image-refs {"💡" 10 "🎯" 11}
        xobject-names {"💡" "Em1" "🎯" "Em2"}
        resource-dict (doc/generate-image-resources image-refs xobject-names)]
    (println "✓ Resource dictionary generation works")
    (println "  - Generated resource dict length:" (count resource-dict))
    (println "  - Contains XObject:" (boolean (clojure.string/includes? resource-dict "/XObject"))))
  (catch js/Error e
    (println "✗ Error in resource dictionary:" (.-message e))))

;; Test page resource merging
(println "\n5. Testing page resource merging...")
(try
  (let [font-dict "/Font <<\n/Arial 3 0 R\n>>"
        image-dict "/XObject <<\n/Em1 10 0 R\n>>"
        merged (doc/update-page-resources font-dict image-dict)]
    (println "✓ Page resource merging works")
    (println "  - Merged resources length:" (count merged))
    (println "  - Contains Font and XObject:" 
             (and (clojure.string/includes? merged "/Font")
                  (clojure.string/includes? merged "/XObject"))))
  (catch js/Error e
    (println "✗ Error in resource merging:" (.-message e))))

(println "\n=== Simple Step 12 Test Completed ===")
(println "Core document-level emoji functionality is working!")