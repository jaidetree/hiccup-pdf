(ns tests.examples.run
  "Test runner for hiccup-pdf document examples"
  (:require [dev.jaide.hiccup-pdf.core :refer [hiccup->pdf-document]]
            [dev.jaide.hiccup-pdf.images :as images]
            [clojure.string :as str]
            [clojure.edn :as edn]
            ["fs" :as fs]
            ["path" :as path]))

(def fixtures-dir "src/test/tests/examples/fixtures")
(def output-dir "target/examples")

(defn read-edn-file
  "Read and parse EDN file"
  [file-path]
  (try
    (-> (fs/readFileSync file-path "utf8")
        (edn/read-string))
    (catch js/Error e
      (println (str "ERROR reading " file-path ": " (.-message e)))
      nil)))

(defn write-pdf-file
  "Write PDF content to file"
  [content file-path]
  (try
    (fs/writeFileSync file-path content)
    (println (str "✓ Generated: " file-path))
    true
    (catch js/Error e
      (println (str "✗ Failed to write " file-path ": " (.-message e)))
      false)))

(defn get-fixture-files
  "Get list of fixture files to process"
  [specific-files]
  (if (seq specific-files)
    ;; Process only specified files
    (map #(str fixtures-dir "/" % ".edn") specific-files)
    ;; Process all .edn files in fixtures directory
    (let [files (fs/readdirSync fixtures-dir)]
      (->> files
           (filter #(str/ends-with? % ".edn"))
           (map #(str fixtures-dir "/" %))))))

(defn extract-base-name
  "Extract base name without path and extension"
  [file-path]
  (-> file-path
      (path/basename)
      (str/replace #"\.edn$" "")))

#_(defn validate-pdf-with-svg
    "Validate PDF by converting to SVG using scripts/pdf2svg"
    [pdf-path]
    (try
      (let [result (cp/spawnSync "./scripts/pdf2svg" [pdf-path]
                                 {:stdio "pipe" :encoding "utf8"})]
        (if (= 0 (.-status result))
          (do
            (println (str "✓ PDF validation successful: " pdf-path))
            true)
          (do
            (println (str "✗ PDF validation failed: " pdf-path))
            (when (.-stderr result)
              (println (str "   Error: " (.-stderr result))))
            false)))
      (catch js/Error e
        (println (str "✗ Validation error for " pdf-path ": " (.-message e)))
        false)))

(defn process-fixture
  "Process a single fixture file: read EDN, generate PDF, validate"
  [fixture-path]
  (println (str "\n=== Processing: " (path/basename fixture-path) " ==="))

  (let [base-name (extract-base-name fixture-path)
        pdf-path (str output-dir "/" base-name ".pdf")
        hiccup-doc (read-edn-file fixture-path)]

    (if hiccup-doc
      (try
        ;; Generate PDF document with image cache for emoji/image support
        (let [start-time (js/Date.now)
              image-cache (images/create-image-cache)
              options {:enable-emoji-images true :image-cache image-cache}
              pdf-content (hiccup->pdf-document hiccup-doc options)
              end-time (js/Date.now)
              duration (- end-time start-time)]

          (println (str "   Generated PDF: " (count pdf-content) " characters in " duration "ms"))

          ;; Write PDF file
          (if (write-pdf-file pdf-content pdf-path)
            ;; Validate PDF with svg conversion
            {:success true :file base-name :size (count pdf-content) :duration duration}
            ;; The validate-pdf-with-svg function requires Inkscape to be installed
            #_(if (validate-pdf-with-svg pdf-path)
                {:success true :file base-name :size (count pdf-content) :duration duration}
                {:success false :file base-name :error "SVG validation failed"})
            {:success false :file base-name :error "Failed to write PDF"}))

        (catch js/Error e
          (println (str "✗ Generation failed: " (.-message e)))
          {:success false :file base-name :error (.-message e)}))

      {:success false :file base-name :error "Failed to read EDN file"})))

(defn -main
  "Main function - process fixture files and generate PDFs"
  [& args]
  (println "=== Hiccup-PDF Examples Test Runner ===")
  (println (str "Output directory: " output-dir))

  ;; Ensure output directory exists
  (when-not (fs/existsSync output-dir)
    (fs/mkdirSync output-dir #js {:recursive true}))

  ;; Get files to process
  (let [fixture-files (get-fixture-files args)
        start-time (js/Date.now)]

    (if (empty? fixture-files)
      (println "No fixture files found!")
      (do
        (println (str "Processing " (count fixture-files) " fixture files..."))

        ;; Process each fixture
        (let [results (mapv process-fixture fixture-files)
              end-time (js/Date.now)
              total-duration (- end-time start-time)
              successful (filter :success results)
              failed (filter #(not (:success %)) results)]

          ;; Print summary
          (println "\n=== SUMMARY ===")
          (println (str "Total files: " (count results)))
          (println (str "Successful: " (count successful)))
          (println (str "Failed: " (count failed)))
          (println (str "Total time: " total-duration "ms"))

          (when (seq successful)
            (println "\n✓ Successfully processed:")
            (doseq [result successful]
              (println (str "  " (:file result) " (" (:size result) " chars, " (:duration result) "ms)"))))

          (when (seq failed)
            (println "\n✗ Failed to process:")
            (doseq [result failed]
              (println (str "  " (:file result) " - " (:error result)))))

          ;; Exit with appropriate code
          (if (seq failed)
            (do
              (println "\nSome tests failed!")
              (js/process.exit 1))
            (do
              (println "\nAll tests passed!")
              (js/process.exit 0))))))))

;; Auto-run when called as script
(apply -main *command-line-args*)
