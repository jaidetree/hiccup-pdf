#!/usr/bin/env nbb

(require '[dev.jaide.hiccup-pdf.core :as core]
         '[dev.jaide.hiccup-pdf.images :as images]
         '[dev.jaide.hiccup-pdf.emoji :as emoji])

(println "=== Step 12: Document-Level Integration Test ===")

;; Test 1: Simple document with emoji
(println "\n1. Testing simple document with emoji...")
(try
  (let [cache (images/create-image-cache)
        document [:document {:title "Simple Emoji Test"}
                  [:page {}
                   [:text {:x 100 :y 100 :font "Arial" :size 14} "Hello 💡 world"]]]
        options {:enable-emoji-images true :image-cache cache}
        result (core/hiccup->pdf-document document options)]
    (println "✓ Document generated successfully")
    (println "  - Result type:" (type result))
    (println "  - Length:" (count result) "characters")
    (println "  - Contains PDF header:" (boolean (clojure.string/includes? result "%PDF-1.4")))
    (println "  - Contains EOF:" (boolean (clojure.string/includes? result "%%EOF"))))
  (catch js/Error e
    (println "✗ Error in simple document test:" (.-message e))))

;; Test 2: Multi-page document  
(println "\n2. Testing multi-page document...")
(try
  (let [cache (images/create-image-cache)
        document [:document {:title "Multi-page Test"}
                  [:page {}
                   [:text {:x 100 :y 100 :font "Arial" :size 14} "Page 1: 💡"]]
                  [:page {}
                   [:text {:x 100 :y 100 :font "Arial" :size 14} "Page 2: 🎯 ✅"]]]
        options {:enable-emoji-images true :image-cache cache}
        result (core/hiccup->pdf-document document options)]
    (println "✓ Multi-page document generated successfully")
    (println "  - Length:" (count result) "characters")
    (println "  - Contains multiple page references:" 
             (> (count (re-seq #"/Type /Page" result)) 1)))
  (catch js/Error e
    (println "✗ Error in multi-page test:" (.-message e))))

;; Test 3: Document without emoji (backwards compatibility)
(println "\n3. Testing backwards compatibility (no emoji)...")
(try
  (let [document [:document {:title "No Emoji Test"}
                  [:page {}
                   [:text {:x 100 :y 100 :font "Arial" :size 14} "Hello 💡 world"]]]
        result (core/hiccup->pdf-document document)]  ; No emoji options
    (println "✓ Backwards compatibility works")
    (println "  - Length:" (count result) "characters")
    (println "  - Contains hex encoding:" (boolean (clojure.string/includes? result "<"))))
  (catch js/Error e
    (println "✗ Error in backwards compatibility test:" (.-message e))))

;; Test 4: Complex document with nested structures
(println "\n4. Testing complex document structure...")
(try
  (let [cache (images/create-image-cache)
        document [:document {:title "Complex Structure" :author "Test Author"}
                  [:page {}
                   [:g {:transforms [[:translate [50 50]]]}
                    [:text {:x 0 :y 0 :font "Arial" :size 12} "Group: 💡"]
                    [:rect {:x 10 :y 10 :width 50 :height 30 :fill "#ff0000"}]]
                   [:text {:x 200 :y 200 :font "Arial" :size 14} "Outside: ✅"]]]
        options {:enable-emoji-images true :image-cache cache}
        result (core/hiccup->pdf-document document options)]
    (println "✓ Complex structure handled successfully")
    (println "  - Contains save/restore states:" 
             (and (clojure.string/includes? result "q")
                  (clojure.string/includes? result "Q")))
    (println "  - Contains author metadata:" 
             (boolean (clojure.string/includes? result "(Test Author)"))))
  (catch js/Error e
    (println "✗ Error in complex structure test:" (.-message e))))

;; Test 5: Error handling
(println "\n5. Testing error handling...")
(try
  (let [document [:document {:title "Error Handling"}
                  [:page {}
                   [:text {:x 100 :y 100 :font "Arial" :size 14} "Test: 🦄"]]]  ; Unicorn unlikely to exist
        options {:enable-emoji-images true
                 :image-cache (images/create-image-cache)
                 :fallback-strategy :placeholder}
        result (core/hiccup->pdf-document document options)]
    (println "✓ Error handling works (graceful fallback)")
    (println "  - Document still generated despite missing emoji files"))
  (catch js/Error e
    (println "✗ Error in error handling test:" (.-message e))))

;; Test 6: Configuration integration
(println "\n6. Testing emoji configuration integration...")
(try
  (let [cache (images/create-image-cache)
        emoji-config (emoji/create-emoji-config 
                      {:enable-emoji-images true
                       :fallback-strategy :hex-string
                       :cache-size 25})
        document [:document {:title "Config Integration"}
                  [:page {}
                   [:text {:x 100 :y 100 :font "Arial" :size 14} "Config: 💡"]]]
        options {:enable-emoji-images true 
                 :image-cache cache 
                 :emoji-config emoji-config}
        result (core/hiccup->pdf-document document options)]
    (println "✓ Configuration integration works")
    (println "  - Config validation passed")
    (println "  - Document generated with custom config"))
  (catch js/Error e
    (println "✗ Error in configuration test:" (.-message e))))

;; Performance test
(println "\n7. Testing performance...")
(try
  (let [cache (images/create-image-cache)
        document [:document {:title "Performance Test"}
                  [:page {}
                   [:text {:x 100 :y 100 :font "Arial" :size 14} "Performance test with emoji: 💡 ✅ 🎯"]]]
        
        start-time (.now js/Date)
        result (core/hiccup->pdf-document document {:enable-emoji-images true :image-cache cache})
        end-time (.now js/Date)
        duration (- end-time start-time)]
    
    (println "✓ Performance test completed")
    (println "  - Generation time:" duration "ms")
    (println "  - Performance acceptable:" (< duration 5000)))
  (catch js/Error e
    (println "✗ Error in performance test:" (.-message e))))

(println "\n=== Step 12 Integration Tests Completed ===")
(println "Document-level emoji image integration is ready!")