# KidGuard

KidGuard est une application mobile Android de contrôle parental développée en Kotlin. Elle a pour objectif d’aider les parents à surveiller l’activité numérique de leurs enfants, à assurer leur sécurité et à détecter les comportements à risque.

## Fonctionnalités

### Côté parent
- Authentification sécurisée via Firebase
- Visualisation des profils enfants
- Géolocalisation en temps réel
- Définition de zones sécurisées (geofencing)
- Blocage automatique après dépassement du temps d’écran
- Accès à la caméra et au micro à distance
- Messagerie avec chaque enfant
- Détection et affichage des messages suspects
- Système de notification en cas d’urgence ou d’activité anormale

### Côté enfant
- Connexion simplifiée
- Création et gestion du profil
- Bouton SOS pour alerter les parents
- Suivi du temps passé sur le téléphone
- Consultation de l’agenda
- Messagerie avec les parents ou les autres enfants autorisés
- Enregistrement audio ou vidéo à la demande du parent

## Technologies utilisées

Le projet utilise les technologies et bibliothèques suivantes :

- Kotlin pour le développement Android natif
- Firebase Authentication pour la gestion des utilisateurs
- Firebase Firestore pour la base de données en temps réel
- Firebase Storage pour le stockage des photos et vidéos
- Google Maps SDK et Places API pour la géolocalisation et les zones
- Perspective API pour la détection des propos inappropriés
- RecyclerView pour l’affichage des listes de messages et utilisateurs
- MediaRecorder pour l’enregistrement audio/vidéo
- SurfaceView pour l’affichage caméra

## Configuration du projet

### Prérequis

- Android Studio (version récente recommandée)
- Un compte Firebase
- Une clé API Google Maps et Places
- (Optionnel) Une clé Perspective API

### Étapes d’installation

1. Cloner le dépôt Git: ```git clone https://github.com/meradamiradjihane/KIDGuard_M_B.git```
3. Créer un projet Firebase :
- Activer l’authentification par email/mot de passe
- Créer la base Firestore et le stockage
- Télécharger le fichier `google-services.json` et le placer dans le dossier `app/`

3. Ajouter les clés API Google dans le fichier `local.properties` ou `AndroidManifest.xml`.

4. Ouvrir le projet dans Android Studio et lancer l’application.

