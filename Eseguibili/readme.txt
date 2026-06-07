Credenziali mockup per il test degli eseguibili
ADMIN:
  USERNAME: admin
  PASSWORD: admin
GIOCATORE 1:
  USERNAME: player1
  PASSWORD: player1
GIOCATORE 2:
  USERNAME: player2
  PASSWORD: player2


Istruzioni per l'utilizzo degli eseguibili .jar
1. Aprire tre terminali (se si supporta il multitab ancora meglio)
2. Sul primo eseguire il GuessTheWordServer.jar
    java -jar /path/to/jar/GuessTheWordServer.jar
3. Alla schermata di login inserire le credenziali admin mockup:
  USERNAME: admin
  PASSWORD: admin
4. Dall'interfaccia Server: File -> Carica Documento; File -> Avvia Analisi
5. Aprire sugli altri due terminali il client allo stesso modo
    java -jar /path/to/jar/GuessTheWordClient.jar
6. Inserire in ciascuna pagina di login le credenziali dei due giocatori
7. Avviare "Nuova Partita" su entrambi i client



