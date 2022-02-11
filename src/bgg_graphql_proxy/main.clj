(ns bgg-graphql-proxy.main
  (:require [clojure.tools.logging :as log]
            [com.walmartlabs.lacinia.pedestal :as lacinia-pedestal]
            [com.walmartlabs.lacinia.schema :as schema]
            [io.pedestal.http :as http]
            [superlifter.api :as s]
            [superlifter.lacinia :refer [inject-superlifter with-superlifter]]))

(def pet-db {"abc-123" {:name "Lyra"
                        :age 11}
             "def-234" {:name "Pantalaimon"
                        :age 11}
             "ghi-345" {:name "Iorek"
                        :age 41}
             "pet1"    {:name "ASDF"
                        :age 100}})

;; without superlifter
#_(defn- resolve-pets [context args parent]
  (log/info "resolve-pets is called")
  (let [ids (keys pet-db)]
    (map (fn [id] {:id id}) ids)))

;; invoked n times, once for every id from the parent resolver
#_(defn- resolve-pet-details [context args {:keys [id]}]
  (log/info "resolve-pet-details is called")
  (get pet-db id))

;; without superlifter

;; with superlifter

(s/def-fetcher FetchPets []
  (fn [_this env]
    #_(log/info env)
    (map (fn [id] {:id id})
         (keys (:db env)))))

(s/def-superfetcher FetchPet [id]
  (fn [many env]
    #_(log/info env)
    (log/info "pet-id를 이용하여 pet-detail을 가져오는 리퀘스트 " (count many) "개 합쳐짐.")
    (map (:db env) (map :id many))))

(defn- resolve-pets [context _args _parent]
  ;; context는 그냥 request정보같은게 있음.
  (with-superlifter context
    (do (log/info "resolve-pets " context _parent)
      (-> (s/enqueue! (->FetchPets))  ;; pets를 일괄로 가져옴. 그걸 큐에 넣음.
          ;; 트리거를 바로 부름.
          ;; 첫번째 인자는  promise
          ;; 이거로 트리거를 재설정한다.
          ;; 트리거 바꾸지 않으니까 하나씩 실행됨. threshold가 0이면 바로 실행되는 듯.
          ;; 이게 enqueue를 하자마자 동작하는 것이 아닌가봄. 좀 느림.
          ;; 그래서 update-trigger! 를 이렇게 수행해도 문제가 없나봄.
          (s/update-trigger! :pet-details                ; bucket-id
                             :elastic                    ; trigger-kind
                             (fn [trigger-opts pet-ids]  ; opts-fn
                               ;; opts-fn으로 동적으로 잠깐 threshold를 바꿀 수도 있음.
                               ;; (println "update-trigger!" trigger-opts pet-ids)
                               ;; pet-id 갯수만큼으로 임계를 바꿔줌으로써 pet-id가 꽉차면 알아서
                               ;; pet-details를 가져옴.
                               (update trigger-opts :threshold + (count pet-ids))))))))

(defn- resolve-pet-details [context _args {:keys [id]}]
  (with-superlifter context
    (do
      (log/info "resolve-pet-details" context id)
      (s/enqueue! :pet-details (->FetchPet id)))))


;; with superlifter
(def schema
  {:objects {:PetDetails {:fields {:name {:type 'String}
                                   :age {:type 'Int}}}
             :Pet {:fields {:id {:type 'String}
                            :details {:type :PetDetails
                                      :resolve resolve-pet-details}}}}
   :queries {:pets
             {:type '(list :Pet)
              :resolve resolve-pets}}})


(defn stop-server
  [server]
  (http/stop server)
  nil)

(def lacinia-opts {:graphiql true
                   :port 8888
                   :join? false})

(def superlifter-args
  {:buckets {:default {:triggers {:queue-size {:threshold 1}}}
             :pet-details {:triggers {:elastic {:threshold 0}}}}
   ;; 이건 내부적으로 쓰는 데이터 env인듯...? 팜모닝에는 일단 db 커넥션을 넣어놓은듯.
   ;; 애초에 superlifter-args를 함수로 바꿔서 context를 처음부터 넣고 있음.
   ;; 거기서 db를 빼서 여기에 넣음.
    :urania-opts {:env {:db pet-db}}})

(def service
  (lacinia-pedestal/service-map
   (fn [] (schema/compile schema))
   (assoc lacinia-opts
          :interceptors (into [(inject-superlifter superlifter-args)]
                              (lacinia-pedestal/default-interceptors (fn [] (schema/compile schema)) lacinia-opts)))))

(comment
  (def runnable-service (http/create-server service))
  (def running-server (http/start runnable-service))
  (http/stop runnable-service)
  (http/stop running-server)
  ;; curl -XPOST -H "Content-Type:application/graphql" localhost:8888/graphql -d '{pets {id details {name}}}'
  )