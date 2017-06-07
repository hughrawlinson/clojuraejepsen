(ns clojuraejepsen.core
  (:require [clojure.string :as str]
            [om.core :as om]
            [om.dom :as dom]))

(enable-console-print!)

;; define your app data so that it doesn't get over-written on reload

(def spotify-api-base-uri "https://api.spotify.com/v1")
(def spotify-accounts-base-uri "https://accounts.spotify.com")
(def spotify-client-id "d048d884a131460d851c9d609e2e1b53")
(def carly-rae-jepsen-spotify-id "6sFIWsNpZYqfjUpaCgueju")

(def spotify-authorization-uri (apply str [spotify-accounts-base-uri
                                           "/authorize?"
                                           "client_id="
                                           spotify-client-id
                                           "&response_type=token"
                                           "&redirect_uri="
                                           (js/encodeURIComponent js/window.location)]))

(defn widget [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/div nil
               (dom/h1 nil (:text data))
               (dom/img #js {:src (:src data) :alt (str "A photo of "(:text data))} nil)))))

(defn pairs [hash] (map #(str/split % "=")
                        (str/split (str/replace-first hash "#" "") #"&")))

(defn auth-code-from-hash
  [hash]
  (reduce (fn [acc el] (merge acc {(keyword (first el)) (second el)})) nil (pairs hash)))

(let [auth-hash (auth-code-from-hash (-> js/window .-location .-hash))]
  (if (some? (:access_token auth-hash))
    (-> (js/fetch
         (str "https://api.spotify.com/v1/artists/" carly-rae-jepsen-spotify-id)
         (clj->js {:method "GET"
                   :headers {:Authorization (str "Bearer " (:access_token auth-hash))}}))
        (.then #(.json %))
        (.then js->clj)
        (.then (fn [resp]
                 (println resp)
                 (om/root widget {:text (get resp "name")
                                  :src (get (first (get resp "images")) "url")}
                          {:target (. js/document (getElementById "app"))}))))
    (set! (.-location js/document) spotify-authorization-uri)))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
