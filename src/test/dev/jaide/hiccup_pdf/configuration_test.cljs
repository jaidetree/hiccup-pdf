(ns dev.jaide.hiccup-pdf.configuration-test
  "Tests for emoji configuration system"
  (:require [cljs.test :refer [deftest is testing]]
            [dev.jaide.hiccup-pdf.emoji :as emoji]))

(deftest test-default-emoji-config
  (testing "Default configuration validity"
    (let [config emoji/default-emoji-config]
      (is (map? config))
      (is (contains? config :enable-emoji-images))
      (is (contains? config :emoji-directory))
      (is (contains? config :fallback-strategy))
      (is (contains? config :cache-size))
      (is (contains? config :cache-memory-mb))
      (is (contains? config :baseline-adjust))
      (is (contains? config :min-font-size))
      (is (contains? config :max-font-size))
      (is (contains? config :spacing-adjust))
      (is (contains? config :debug))
      (is (contains? config :validation?))
      (is (contains? config :logging?))))
  
  (testing "Default values are sensible"
    (let [config emoji/default-emoji-config]
      (is (= false (:enable-emoji-images config)))  ; Disabled by default
      (is (= "emojis/noto-72/" (:emoji-directory config)))
      (is (= :hex-string (:fallback-strategy config)))
      (is (= 50 (:cache-size config)))
      (is (= 10 (:cache-memory-mb config)))
      (is (= 0.2 (:baseline-adjust config)))
      (is (= 8 (:min-font-size config)))
      (is (= 72 (:max-font-size config)))
      (is (= 1.0 (:spacing-adjust config)))
      (is (= false (:debug config)))
      (is (= true (:validation? config)))
      (is (= true (:logging? config)))))
  
  (testing "Default config validates successfully"
    (let [validation (emoji/validate-emoji-config emoji/default-emoji-config)]
      (is (:valid? validation))
      (is (empty? (:errors validation))))))

(deftest test-validate-emoji-config
  (testing "Valid configuration passes validation"
    (let [valid-config {:enable-emoji-images true
                        :emoji-directory "custom/path/"
                        :fallback-strategy :placeholder
                        :cache-size 100
                        :cache-memory-mb 20
                        :baseline-adjust 0.3
                        :min-font-size 10
                        :max-font-size 48
                        :spacing-adjust 1.5
                        :debug true
                        :validation? false
                        :logging? true}
          validation (emoji/validate-emoji-config valid-config)]
      (is (:valid? validation))
      (is (empty? (:errors validation)))))
  
  (testing "Invalid enable-emoji-images"
    (let [invalid-config {:enable-emoji-images "yes"}
          validation (emoji/validate-emoji-config invalid-config)]
      (is (not (:valid? validation)))
      (is (some #(clojure.string/includes? % ":enable-emoji-images must be true or false") (:errors validation)))))
  
  (testing "Invalid emoji-directory"
    (let [validation1 (emoji/validate-emoji-config {:emoji-directory 123})
          validation2 (emoji/validate-emoji-config {:emoji-directory ""})]
      (is (not (:valid? validation1)))
      (is (some #(clojure.string/includes? % ":emoji-directory must be a string") (:errors validation1)))
      (is (not (:valid? validation2)))
      (is (some #(clojure.string/includes? % ":emoji-directory cannot be empty") (:errors validation2)))))
  
  (testing "Invalid fallback-strategy"
    (let [invalid-config {:fallback-strategy :invalid-strategy}
          validation (emoji/validate-emoji-config invalid-config)]
      (is (not (:valid? validation)))
      (is (some #(clojure.string/includes? % ":fallback-strategy must be one of") (:errors validation)))))
  
  (testing "Invalid cache-size"
    (let [validation1 (emoji/validate-emoji-config {:cache-size -5})
          validation2 (emoji/validate-emoji-config {:cache-size "fifty"})]
      (is (not (:valid? validation1)))
      (is (some #(clojure.string/includes? % ":cache-size must be a positive number") (:errors validation1)))
      (is (not (:valid? validation2)))
      (is (some #(clojure.string/includes? % ":cache-size must be a positive number") (:errors validation2)))))
  
  (testing "Invalid cache-memory-mb"
    (let [invalid-config {:cache-memory-mb 0}
          validation (emoji/validate-emoji-config invalid-config)]
      (is (not (:valid? validation)))
      (is (some #(clojure.string/includes? % ":cache-memory-mb must be a positive number") (:errors validation)))))
  
  (testing "Invalid baseline-adjust"
    (let [invalid-config {:baseline-adjust "0.2"}
          validation (emoji/validate-emoji-config invalid-config)]
      (is (not (:valid? validation)))
      (is (some #(clojure.string/includes? % ":baseline-adjust must be a number") (:errors validation)))))
  
  (testing "Invalid font sizes"
    (let [validation1 (emoji/validate-emoji-config {:min-font-size -1})
          validation2 (emoji/validate-emoji-config {:max-font-size 0})
          validation3 (emoji/validate-emoji-config {:min-font-size 20 :max-font-size 10})]
      (is (not (:valid? validation1)))
      (is (some #(clojure.string/includes? % ":min-font-size must be a positive number") (:errors validation1)))
      (is (not (:valid? validation2)))
      (is (some #(clojure.string/includes? % ":max-font-size must be a positive number") (:errors validation2)))
      (is (not (:valid? validation3)))
      (is (some #(clojure.string/includes? % ":min-font-size must be less than :max-font-size") (:errors validation3)))))
  
  (testing "Invalid spacing-adjust"
    (let [invalid-config {:spacing-adjust -0.5}
          validation (emoji/validate-emoji-config invalid-config)]
      (is (not (:valid? validation)))
      (is (some #(clojure.string/includes? % ":spacing-adjust must be a non-negative number") (:errors validation)))))
  
  (testing "Invalid boolean flags"
    (let [configs [{:debug "true"}
                   {:validation? 1}
                   {:logging? "false"}]]
      (doseq [config configs]
        (let [validation (emoji/validate-emoji-config config)]
          (is (not (:valid? validation)))))))
  
  (testing "Warnings for extreme values"
    (let [config-large-cache {:cache-size 300}
          validation1 (emoji/validate-emoji-config config-large-cache)]
      (is (:valid? validation1))  ; Valid but with warnings
      (is (some #(clojure.string/includes? % "Large cache-size may impact memory usage") (:warnings validation1))))
    
    (let [config-extreme-baseline {:baseline-adjust -0.8}
          validation2 (emoji/validate-emoji-config config-extreme-baseline)]
      (is (:valid? validation2))  ; Valid but with warnings
      (is (some #(clojure.string/includes? % "Extreme baseline-adjust values") (:warnings validation2)))))
  
  (testing "Multiple errors reported"
    (let [invalid-config {:enable-emoji-images "maybe"
                          :cache-size -10
                          :fallback-strategy :unknown}
          validation (emoji/validate-emoji-config invalid-config)]
      (is (not (:valid? validation)))
      (is (>= (count (:errors validation)) 3))))  ; Should report multiple errors
  
  (testing "Empty config validates successfully"
    (let [validation (emoji/validate-emoji-config {})]
      (is (:valid? validation))
      (is (empty? (:errors validation)))))
  
  (testing "Nil config handled gracefully"
    (let [validation (emoji/validate-emoji-config nil)]
      (is (:valid? validation)))))

(deftest test-merge-emoji-config
  (testing "Merging with empty user config"
    (let [merged (emoji/merge-emoji-config {})]
      (is (= emoji/default-emoji-config merged))))
  
  (testing "Merging with nil user config"
    (let [merged (emoji/merge-emoji-config nil)]
      (is (= emoji/default-emoji-config merged))))
  
  (testing "Merging partial user config"
    (let [user-config {:enable-emoji-images true :cache-size 75}
          merged (emoji/merge-emoji-config user-config)]
      (is (= true (:enable-emoji-images merged)))
      (is (= 75 (:cache-size merged)))
      (is (= (:emoji-directory emoji/default-emoji-config) (:emoji-directory merged)))
      (is (= (:fallback-strategy emoji/default-emoji-config) (:fallback-strategy merged)))))
  
  (testing "Merging complete user config"
    (let [user-config {:enable-emoji-images true
                       :emoji-directory "custom/"
                       :fallback-strategy :skip
                       :cache-size 200
                       :cache-memory-mb 50
                       :baseline-adjust 0.1
                       :min-font-size 6
                       :max-font-size 96
                       :spacing-adjust 2.0
                       :debug true
                       :validation? false
                       :logging? false}
          merged (emoji/merge-emoji-config user-config)]
      (is (= user-config merged))))
  
  (testing "Invalid user config throws error"
    (let [invalid-config {:enable-emoji-images "not-boolean"}]
      (is (thrown? js/Error (emoji/merge-emoji-config invalid-config)))))
  
  (testing "User config overrides defaults"
    (let [user-config {:cache-size 25 :debug true}
          merged (emoji/merge-emoji-config user-config)]
      (is (= 25 (:cache-size merged)))
      (is (= true (:debug merged)))
      ;; Defaults should still be present
      (is (= false (:enable-emoji-images merged)))))
  
  (testing "Error message contains helpful information"
    (try
      (emoji/merge-emoji-config {:fallback-strategy :invalid})
      (is false "Should have thrown error")
      (catch js/Error e
        (is (clojure.string/includes? (.-message e) "Invalid emoji configuration"))
        (is (clojure.string/includes? (.-message e) ":fallback-strategy"))))))

(deftest test-emoji-config-enabled?
  (testing "Feature enabled"
    (let [config {:enable-emoji-images true}]
      (is (= true (emoji/emoji-config-enabled? config)))))
  
  (testing "Feature disabled"
    (let [config {:enable-emoji-images false}]
      (is (= false (emoji/emoji-config-enabled? config)))))
  
  (testing "Feature not specified (defaults to false)"
    (let [config {}]
      (is (= false (emoji/emoji-config-enabled? config)))))
  
  (testing "Nil config"
    (is (= false (emoji/emoji-config-enabled? nil))))
  
  (testing "Non-boolean value coerced to boolean"
    (let [config {:enable-emoji-images "true"}]
      (is (= true (emoji/emoji-config-enabled? config))))
    (let [config {:enable-emoji-images 0}]
      (is (= false (emoji/emoji-config-enabled? config))))))

(deftest test-get-emoji-config-value
  (testing "Get existing value"
    (let [config {:cache-size 100}]
      (is (= 100 (emoji/get-emoji-config-value config :cache-size)))))
  
  (testing "Get missing value with default"
    (let [config {}]
      (is (= 50 (emoji/get-emoji-config-value config :cache-size)))))  ; Default cache-size
  
  (testing "Nil config uses defaults"
    (is (= "emojis/noto-72/" (emoji/get-emoji-config-value nil :emoji-directory))))
  
  (testing "Unknown key returns nil"
    (let [config {}]
      (is (= nil (emoji/get-emoji-config-value config :unknown-key)))))
  
  (testing "Config value overrides default"
    (let [config {:baseline-adjust 0.5}]
      (is (= 0.5 (emoji/get-emoji-config-value config :baseline-adjust)))))
  
  (testing "All default keys are accessible"
    (let [default-keys (keys emoji/default-emoji-config)]
      (doseq [key default-keys]
        (let [value (emoji/get-emoji-config-value nil key)]
          (is (not (nil? value))))))))

(deftest test-create-emoji-config
  (testing "Create config from valid options"
    (let [options {:enable-emoji-images true :cache-size 80}
          config (emoji/create-emoji-config options)]
      (is (= true (:enable-emoji-images config)))
      (is (= 80 (:cache-size config)))
      (is (= (:emoji-directory emoji/default-emoji-config) (:emoji-directory config)))))
  
  (testing "Create config from empty options"
    (let [config (emoji/create-emoji-config {})]
      (is (= emoji/default-emoji-config config))))
  
  (testing "Invalid options throw error"
    (is (thrown? js/Error (emoji/create-emoji-config {:cache-size -5}))))
  
  (testing "Create config is same as merge-emoji-config"
    (let [options {:debug true :fallback-strategy :placeholder}
          config1 (emoji/create-emoji-config options)
          config2 (emoji/merge-emoji-config options)]
      (is (= config1 config2)))))

(deftest test-emoji-config->cache-config
  (testing "Extract cache configuration"
    (let [emoji-config {:cache-size 75 :cache-memory-mb 15 :other-option true}
          cache-config (emoji/emoji-config->cache-config emoji-config)]
      (is (= {:max-size 75 :max-memory-mb 15} cache-config))))
  
  (testing "Missing values use defaults"
    (let [emoji-config {:other-option true}
          cache-config (emoji/emoji-config->cache-config emoji-config)]
      (is (= {:max-size 50 :max-memory-mb 10} cache-config))))
  
  (testing "Empty config uses defaults"
    (let [cache-config (emoji/emoji-config->cache-config {})]
      (is (= {:max-size 50 :max-memory-mb 10} cache-config))))
  
  (testing "Default emoji config produces expected cache config"
    (let [cache-config (emoji/emoji-config->cache-config emoji/default-emoji-config)]
      (is (= {:max-size 50 :max-memory-mb 10} cache-config)))))

(deftest test-print-emoji-config
  (testing "Print default configuration"
    (let [output (emoji/print-emoji-config emoji/default-emoji-config)]
      (is (string? output))
      (is (clojure.string/includes? output "Emoji Configuration:"))
      (is (clojure.string/includes? output "Enabled: false"))
      (is (clojure.string/includes? output "Directory: emojis/noto-72/"))
      (is (clojure.string/includes? output "Fallback: hex-string"))
      (is (clojure.string/includes? output "Cache Size: 50"))
      (is (clojure.string/includes? output "Cache Memory: 10MB"))))
  
  (testing "Print custom configuration"
    (let [custom-config {:enable-emoji-images true
                         :emoji-directory "custom/"
                         :fallback-strategy :placeholder
                         :cache-size 100
                         :cache-memory-mb 25
                         :baseline-adjust 0.3
                         :min-font-size 10
                         :max-font-size 48
                         :spacing-adjust 1.5
                         :debug true
                         :validation? false
                         :logging? true}
          output (emoji/print-emoji-config custom-config)]
      (is (clojure.string/includes? output "Enabled: true"))
      (is (clojure.string/includes? output "Directory: custom/"))
      (is (clojure.string/includes? output "Fallback: placeholder"))
      (is (clojure.string/includes? output "Cache Size: 100"))
      (is (clojure.string/includes? output "Font Size Range: 10-48"))))
  
  (testing "Print nil config uses defaults"
    (let [output (emoji/print-emoji-config nil)]
      (is (string? output))
      (is (clojure.string/includes? output "Enabled: false"))))
  
  (testing "Output contains all configuration sections"
    (let [output (emoji/print-emoji-config emoji/default-emoji-config)
          sections ["Enabled:" "Directory:" "Fallback:" "Cache Size:" "Cache Memory:" 
                   "Baseline Adjust:" "Font Size Range:" "Spacing Adjust:" "Debug:" 
                   "Validation:" "Logging:"]]
      (doseq [section sections]
        (is (clojure.string/includes? output section))))))

(deftest test-configuration-integration
  (testing "Configuration works with validation"
    (let [user-config {:enable-emoji-images true :cache-size 60}
          merged-config (emoji/merge-emoji-config user-config)
          validation (emoji/validate-emoji-config merged-config)]
      (is (:valid? validation))
      (is (empty? (:errors validation)))
      (is (emoji/emoji-config-enabled? merged-config))))
  
  (testing "Configuration error propagation"
    (let [invalid-config {:cache-size "not-a-number"}]
      (is (thrown? js/Error (emoji/merge-emoji-config invalid-config)))))
  
  (testing "Configuration accessor functions work together"
    (let [config (emoji/create-emoji-config {:debug true :cache-size 90})]
      (is (= true (emoji/get-emoji-config-value config :debug)))
      (is (= 90 (emoji/get-emoji-config-value config :cache-size)))
      (is (= false (emoji/emoji-config-enabled? config)))  ; Still disabled
      (is (= {:max-size 90 :max-memory-mb 10} (emoji/emoji-config->cache-config config)))))
  
  (testing "Round-trip configuration preservation"
    (let [original-config {:enable-emoji-images true
                          :fallback-strategy :skip
                          :cache-size 75
                          :baseline-adjust 0.25}
          merged-config (emoji/merge-emoji-config original-config)
          cache-config (emoji/emoji-config->cache-config merged-config)]
      ;; Original values should be preserved
      (is (= true (:enable-emoji-images merged-config)))
      (is (= :skip (:fallback-strategy merged-config)))
      (is (= 75 (:cache-size merged-config)))
      (is (= 0.25 (:baseline-adjust merged-config)))
      ;; Cache config should reflect the settings
      (is (= 75 (:max-size cache-config)))))
  
  (testing "Performance with many validations"
    (let [configs (for [i (range 100)]
                   {:cache-size (+ 10 i) :debug (even? i)})
          start-time (.now js/Date)
          results (doall (map emoji/validate-emoji-config configs))
          end-time (.now js/Date)
          duration (- end-time start-time)]
      (is (every? :valid? results))
      (is (< duration 1000))  ; Should complete within 1 second
      (is (= 100 (count results))))))