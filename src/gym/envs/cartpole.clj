(ns gym.envs.cartpole
  (:require
    [engine.gameloop :as gameloop]
    [engine.physics :refer :all]))

(def MAX-ANGLE 0.20)

(def CART-SPEED 22.75)

(def MOTOR-FORCE 1500.0)

(defn spawn [state]
  (create-body:rectangle [[0 104] [4 4]] :tip)
  (create-body:rectangle [[0 50] [1 100]] :pole)
  (create-joint:weld :tip-rod :pole :tip [0.0 102.0])
  (translate-bodies [:pole :tip] [200 -294])

  (create-body:rectangle [[-150.0 -50] [25 100]] :pillar-left :infinite)
  (create-body:rectangle [[150.0 -50] [25 100]] :pillar-right :infinite)
  (create-body:rectangle [[-156.25 5] [12.5 10]] :pillar-leftmost :infinite)
  (create-body:rectangle [[156.50 5] [12.5 10]] :pillar-rightmost :infinite)
  (translate-bodies
   [:pillar-leftmost :pillar-left :pillar-right :pillar-rightmost] [200 -300])

  (create-body:rectangle [[0 0] [300 2.0]] :rail)

  (create-body:rectangle [[0 0] [30 10]] :cart)
  (create-body:rectangle [[-3 6] [4 4]] :constraint-left)
  (create-body:rectangle [[3 6] [4 4]] :constraint-right)

  (create-joint:weld :weld1 :cart :constraint-left [-3.0 6.0])
  (create-joint:weld :weld2 :cart :constraint-right [3.0 6.0])

  (create-joint:prismatic :prismatic :rail :cart [-50.0 0.0] [50.0 0.0])
  (set-maximum-motor-force :prismatic MOTOR-FORCE)
  (translate-bodies [:rail] [200 -299])
  (translate-bodies [:cart :constraint-left :constraint-right] [200 -304])

  (merge {:step 0
          :max-angle MAX-ANGLE} state))

(defn reset [state]
  (remove-bodies-and-joints)
  (spawn state))

(defn handle-control [{max-angle :max-angle :as state} cmd notify]
  (do
    (cond
      (= cmd :left)
      (set-motor-speed :prismatic CART-SPEED)
      (= cmd :right)
      (set-motor-speed :prismatic (- CART-SPEED))
      (= cmd :stay) (set-motor-speed :prismatic 0))
    (let [state
          (assoc state :observation
                 [(- (first (get-position :cart)) 200.0)
                  (get-rotation :pole)
                  (first (get-linear-velocity :cart))
                  (first (get-linear-velocity :tip))])]
      (-> state
          (update :step inc)
          (cond-> (or (> (-> state :observation second) max-angle)
                      (< (-> state :observation second) (- max-angle)))
            (assoc :done true))
          (notify)))))

;; to be consumed by exercise:
;;
;; :step          - steps after last reset, beginning with 0
;; :done          - when the rod reached a specified maximum angle
;; :keys-pressed
;; :observation
;;   [cart-position
;;    pole-rotation
;;    cart-velocity - x component of the tip's velocity
;;    tip-velocity] - x component of the tip's velocity
;;
;; to control the environment:
;;
;; :max-angle to override the default MAX-ANGLE
;; :cmd
;;   :end   - ends processing
;;   :reset - resets the environment
;;   :left  - moves the cart to the left
;;   :right - moves the cart to the right
;;
(defn on-tick-observable [notify notify-end]
  (fn on-tick [{cmd :cmd finished :finished :as state} keys-pressed tick-in-ms]
    (if finished
      state
      (cond
        (= :reset cmd)
        (do (reset state)
          (-> state
              (assoc :cmd nil)
              (assoc :done false)
              (assoc :step 0)
              (notify)))
        (= :end cmd)
        (do
          (-> state
              (assoc :finished true)
              (notify-end)))
        :else
        (handle-control (assoc state :keys-pressed keys-pressed) cmd notify)))))

(defn go [on-tick-observer on-end-observer initial-state]
  (gameloop/start (spawn initial-state) (on-tick-observable on-tick-observer on-end-observer)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; dev mode - cart can be controlled manually

(defn on-tick [{keys-pressed :keys-pressed
                done         :done
                :as          state}]
  (if done
    (assoc state :cmd :reset)
    (cond
      (.contains keys-pressed \a)
      (assoc state :cmd :left)
      (.contains keys-pressed \d)
      (assoc state :cmd :right))))

(defn -main [& args]
  (go on-tick identity {}))