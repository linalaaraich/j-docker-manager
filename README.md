# J-Docker Remote Manager

## 👥 Équipe

**Réalisé par :**
- ESSAFI Fatima-Ezzahrae
- LAARAICH Lina
- BOURRAT Yosra
- NACIRI Fatima Zahra

**Encadré par :**
- Pr. Ahmed BENTAJER

**Gestion distribuée de conteneurs Docker via architecture client-serveur TCP**

---

## 📋 Description

J-Docker Remote Manager est une application Java permettant de gérer à distance un moteur Docker via une architecture client-serveur basée sur TCP. Le projet offre une interface CLI intuitive pour piloter l'ensemble du cycle de vie des conteneurs Docker sans accès direct à la machine hôte.

### Fonctionnalités principales

- **Gestion des images** : Liste et téléchargement d'images depuis Docker Hub
- **Cycle de vie des conteneurs** : Création, démarrage, arrêt et suppression
- **Architecture multithreadée** : Support de plusieurs clients simultanés
- **Protocole JSON** : Communication structurée et extensible
- **Robustesse** : Gestion complète des erreurs et déconnexions

---

## 🏗️ Architecture

```
┌─────────────┐         TCP/JSON         ┌─────────────┐         Docker API        ┌─────────────┐
│             │  ◄────────────────────►  │             │  ◄────────────────────►   │             │
│   Client    │      Port 9999           │   Serveur   │      Port 2375            │   Docker    │
│    (CLI)    │                          │   (Daemon)  │                           │   Engine    │
│             │                          │             │                           │             │
└─────────────┘                          └─────────────┘                           └─────────────┘
```

---

## 🚀 Utilisation rapide

### Démarrer le serveur
```bash
java -jar docker-server.jar
```

### Connecter un client
```bash
java -jar docker-client.jar
```

### Commandes disponibles
```
images              - Liste toutes les images Docker
pull <image>        - Télécharge une image depuis Docker Hub
ps [-a]            - Liste les conteneurs (actifs ou tous)
create <img> <n>   - Crée un nouveau conteneur
start <id>         - Démarre un conteneur
stop <id>          - Arrête un conteneur
rm <id>            - Supprime un conteneur
status <id>        - Affiche l'état détaillé d'un conteneur
help               - Affiche l'aide
exit               - Déconnexion
```

---

## 🛠️ Technologies

- **Java 17** - Langage de programmation
- **Maven 3.9.9** - Gestion des dépendances
- **Docker Java API 3.3.0** - Interface avec Docker
- **Gson 2.10.1** - Sérialisation JSON
- **Architecture multithreadée** - ExecutorService avec pool de threads

