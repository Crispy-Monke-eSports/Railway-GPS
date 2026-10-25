# Railway GPS

**Produkt- och teknisk målbild**

**Målplattform:** Minecraft Java Edition 26.3, Fabric

**Status:** Sammanhängande planeringsspecifikation för nästa arkitektur

Den här specifikationen beskriver den önskade lösningen utan krav på bakåtkompatibilitet med en tidigare datamodell. Exakta kommandon och lagringsscheman får förfinas under implementation, men domänmodellen och användarbeteendet nedan är vägledande.

**Fastställda produktbeslut (2026-10-25):**
- **UI först, kommandon i andra hand:** det **SimpleBlueprints-inspirerade UI-systemet från commit [`821880e`](https://github.com/Crispy-Monke-eSports/Railway-GPS/commit/821880e7f3909d3fee1bbf84063b51e3c949f703)** är moddets huvudsakliga ingång och arbetsflöde för **både spelare och alla fyra behörighetsroller**. Alla vanliga åtgärder ska gå att genomföra från in-game-UI och visuella verktyg, **utan kommandon eller tekniska ID:n**. Kommandon är **sekundära alternativ** för automation, felsökning och avancerad användning; de får inte vara ett obligatoriskt steg för spel eller administration.
- **Tre enkla användarbegrepp:** stationer (enda destinationerna), rödljus (vägval i en black box) och namn på valfria rälssträckor (t.ex. Riksettan och Centralbron). **Ingen separat terminaltyp, inget plattformssystem och inga extra obligatoriska stationsobjekt.**
- **Station:** ett namngivet stoppställe direkt på rälsen, där spåret tar slut eller där vagnen kan stanna och sedan fortsätta/vända. **Stationsnamn måste vara unika på hela servern, även mellan dimensioner** (jämför efter trimning utan skillnad på stora/små bokstäver). **Hela den markerade stationszonen räknas som stationen, inklusive samtliga rälslinjer innanför.** Ankomst när minecarten når zonen via en verifierad giltig rutt bekräftas **även när vagnen rullar**; inget manuellt spårval i stationszonen krävs. Visa **Framme** tills spelaren lämnar minecarten; avsluta då navigeringen automatiskt.
- **Osäker position:** visa destination och **Osäker position**, men **inga sväng- eller knappinstruktioner** förrän rätt spår identifierats.
- **Survey:** starta var som helst på rälsen; hela inspelningen får publiceras **direkt och atomiskt** vid `finish` bara när anslutningen till det registrerade nätet och övrig validering är säkra. Ingen obligatorisk manuell publiceringsbekräftelse.
- **Parallella spårpar:** geometribaserade förslag, alltid **uttryckligt godkännande från administratör**; annars förblir spåren självständiga.
- **HUD-namn:** visa en **huvudled plus högst ett lokalt namn**, t.ex. `Riksettan · Centralbron`, när de finns.
- **Administratörsinstruktioner:** visas **ordagrant**, oberoende av spelarens språk; endast inbyggda standardtexter översätts.
- **Behörigheter:** separata serverrättigheter för inspektion, namngivning, inspelning samt administration/publicering.
- **Rödljus:** endast in-/utgående spår, verifierade vägval och instruktioner. Ingen intern räls, redstone-modell eller genomfartskostnad.
- **Destination före ombordstigning:** spelaren får välja en station **i sin aktuella dimension** i förväg. Valet förblir väntande tills spelaren sitter i en minecart och **verklig, stabil rörelse på säkert identifierad registrerad räls visar färdriktningen**; först då aktiveras ruttberäkning och vägledning. Inga väganvisningar till fots eller i en stillastående vagn.
- **Avstigning:** **all** avstigning avslutar en aktiv navigation omedelbart, även **före** ankomst; automatisk återupptagning ska inte ske.
- **Rödljusinstruktionens tidpunkt:** visa vägvalet när ett rödljus **börjar närma sig**, behåll instruktionen under passagen genom dess zon och avsluta/byta den när rätt utgående räls har bekräftats; vid osäker position gäller alltid regeln om att dölja instruktioner.
- **Ruttval:** välj automatiskt den **kortaste verifierade, giltiga rutten** utifrån återstående fysisk rälssträcka, med respekt för riktning och tillåtna rödljusövergångar. **Ingen riktning på ett fysiskt spår antas vara körbar utan inspelad eller uttryckligen administratörsgodkänd verifiering**, även om samma räls går att färdas på åt andra hållet i Minecraft. Namngivna leder prioriteras inte och spelaren behöver inte välja mellan alternativa rutter.
- **Nätverksändring under resa:** beräkna automatiskt om den **kortaste återstående giltiga vägen** när den aktiva nätverksrevisionen eller en spår-/övergångsstatus ändras. **Om ingen giltig väg till stationen finns, visa ett tydligt felmeddelande**; fortsätt inte att visa en ogiltig rutt eller felaktiga svänginstruktioner.
- **Namngivning:** behörig användare **markerar sträckans början och slut på befintlig inspelad räls och anger ett namn**. Förhandsvisa exakt träffad räls/sträcka och begär val om flera olika sträckor är möjliga. **Ingen särskild kör-/minecart-inspelning för namngivning** ska implementeras; fysisk survey för att registrera räls är en annan funktion.
- **Ankomstriktning:** en station kan nås **från båda håll** på sin anslutna fysiska räls **om respektive färdriktning redan är verifierad** genom inspelning eller administratörsgodkännande. Ingen särskild stationsankomstriktning ska behöva anges; motsatt färd godkänns aldrig automatiskt.
- **HUD-avstånd:** under aktiv och säker navigering visas **både återstående avstånd till nästa rödljus och till destinationsstationen** i block, mätta längs planerad fysisk räls. Finns inget ytterligare rödljus visas ingen vilseledande siffra för det avståndet; vid osäker position döljs otillförlitliga avstånd.
- **Världsrutt:** vid **första användningen** visas **bara HUD**, utan 3D-rutt i Minecraft-världen. Spelaren kan själv slå på/av den planerade rutten utan JourneyMap; **det lokala personliga valet sparas mellan resor och spelstarter**. Den ritade rutten släcks när navigeringen avslutas, men det sparade valet ändras inte.
- **Stillastående minecart:** ett valt stationsmål förblir väntande medan vagnen står stilla. GPS:en börjar inte vägleda förrän vagnen **faktiskt rör sig med säkert identifierad riktning på registrerad räls**. Spelarens blick, valfri riktning eller förmodad kortaste väg får inte användas för att gissa färdriktning.
- **Namngivning av godkänt parallellt spårpar:** när ett annat fysiskt spår **redan är uttryckligen godkänt som par av administratör** erbjuds ett enkelt **ja/nej-val** om samma namn även ska gälla den rälsen. **Nej är förvalt**; annars namnges bara markerad räls. Inget val visas utan godkänt par, och namngivningsrollen får inte ändra parning eller riktningar.
- **Notis vid omräkning:** när en aktiv rutt **faktiskt byts till en ny giltig rutt**, visa kort den lokaliserade texten **`Rutt omräknad`**, sedan nya instruktioner. Notisen kräver ingen bekräftelse och pausar ingenting. Om ingen rutt finns visas tidigare beslutade felet **`Ingen giltig väg till stationen`** i stället.
- **Skapa station:** administratören **markerar två motsatta hörn** för **en stationszon** och anger ett serverunikt namn. **Allt inom zonen hör till stationen, även om flera separata rälslinjer passerar där.** Ingen räls behöver pekas ut som stationens enda spår och flera spår är **inte** ett valideringsfel. Ingen plattform, terminal, delzon eller extra spårvalsmeny behövs. En zon skapar inte automatiskt kopplingar mellan skilda spår.
- **Byta destination under resa:** spelaren får när som helst under aktiv färd **välja en annan station** och får då **direkt** en ny **kortaste verifierade giltiga rutt** från den säkert observerade aktuella positionen/riktningen, utan att först avsluta resan. Bytet ersätter den gamla destinationen/rutten; ingen ny resetyp införs och ingen användarbekräftelse krävs.
- **Ingen giltig väg vid start eller destinationsbyte:** visa **`Ingen giltig väg till stationen`** och **behåll den valda stationen** som väntande resmål; **försök automatiskt igen när position/riktning kan verifieras på en ny giltig passage eller nätverket uppdaterats så att en rutt kan finnas**. Inga felaktiga körinstruktioner, inga ständiga omförsök varje tick och inget krav på manuellt `Försök igen`. **All avstigning avslutar även denna väntan** enligt tidigare beslut.
- **Borttagen destinationsstation:** om den **valda stationens ID försvinner** ur en fullständigt verifierad serverrevision **under pågående resa**, avsluta navigationen direkt, rensa aktiv rutt/överlägg och visa en **tydlig lokaliserbar notis**, exempelvis `Stationen har tagits bort`. Behandla inte borttagen station som tillfälligt `Ingen giltig väg`, och välj aldrig en ersättningsstation automatiskt.
- **Destinationer i aktuell dimension:** visa och tillåt **endast stationer i spelarens aktuella Minecraft-dimension** som destinationsval, oavsett om andra dimensioner innehåller registrerade stationer och oavsett om tekniska övergångar finns. Samma filtrering gäller **navigationsmeny, kommandon och eventuell JourneyMap-stationsväljare**. Stationsnamn förblir trots detta **unika serverövergripande**.
- **Sparad visningsinställning:** den vanliga spelarens **Visa planerad 3D-rutt** är **av vid första användning**, men därefter ska spelarens val sparas **lokalt per spelare** mellan resor och spelstarter. Att avsluta resan rensar den ritade rutten, **inte** inställningen; när nästa giltiga resa startar visas rutten automatiskt igen om inställningen redan är på. Ingen serveradministratör ska behöva slå på vyn och JourneyMap är oberoende.

## 1. Vision

Railway GPS är ett icke-ingripande navigationsmod för stora befintliga järnvägsnät. Spelaren väljer en **station** och får hjälp att följa rätt spår genom **rödljus**. Administratörer kan **namnge särskilda rälssträckor**. Det krävs inga andra spelarsynliga platstyper, som terminaler eller plattformar. Rödljus är aldrig destinationer.

Moddet ska kunna ge instruktioner som:

- **Fortsätt rakt fram**
- **Fortsätt åt vänster**
- **Fortsätt åt höger**
- **Vänd tillbaka**
- **Fortsätt på Riksettan**
- **Fortsätt åt vänster in på Hamnleden**
- **Välj knappen för Martyrön**

Moddet ska inte styra minecarten, trycka på knappar, ändra redstone, lägga om växlar eller på annat sätt påverka vanilla-järnvägen. Det observerar färden och visar navigationshjälp.

Servern äger och validerar järnvägsnätet. Klienten tar emot en säker, versionsmärkt vy av nätet och ansvarar för presentation, positionsmatchning och navigeringsupplevelse. JourneyMap är en valfri visualisering, inte ett krav.

**Terminologi:** Platsobjektet för vägval heter **rödljus** på svenska och **redlight** på engelska, även i scheman, kommandodesign, gränssnitt och acceptanskriterier. Det finns ingen separat objekttyp eller äldre alternativ benämning för dessa vägval i den nya specifikationen. Befintliga Java-identifierare kan fortfarande bära äldre namn tills koden migreras.

## 2. Grundprinciper

### 2.1 Fysik, vägval och namn är separata

Systemet ska skilja mellan:

1. **Fysisk räls:** var de faktiska spåren går.
2. **Tillåtna övergångar:** vilka vägval som går att göra i en rödljus.
3. **Semantiska namn:** exempelvis Riksettan, Centralbron eller Södra tunneln.
4. **Beräknad resa:** den kombination av riktade spår och övergångar som leder till spelarens mål.

Ett namn får aldrig vara nödvändigt för att ett spår ska kunna användas. Ett namn får börja eller sluta mitt på en fysisk spårsektion och kan fortsätta genom flera rödljus.

### 2.2 Interna ID:n visas inte för spelaren

Alla objekt har stabila tekniska ID:n. Visningsnamn är valfria där de inte behövs för spelupplevelsen. Tekniska namn som `section_0042` eller `track_a81f` ska inte visas i vanlig HUD.

### 2.3 Spelaren ska aldrig behöva förstå datamodellen

Det **SimpleBlueprints-inspirerade in-game-UI:t (commit `821880e`) är primärt** för spelare och administratörer: spelaren söker och väljer station, ändrar/avslutar resan och gör visningsinställningar i UI; behöriga användare inspekterar nätet, markerar namnsträckor, startar/avslutar survey och utför redlight-/stationsredigering via UI och verktyg i världen. Allt detta fungerar **utan att skriva ett enda kommando eller tekniskt ID**. Kommandon är sekundära sätt att anropa **samma validerade serverfunktioner**. Att en funktion existerar som kommando räcker inte för att dess UI-stöd ska anses implementerat.

### 2.4 Osäkerhet ska visas, inte döljas

När två spår inte kan särskiljas säkert ska HUD visa den valda **destinationen** och **Osäker position**. Den ska **dölja alla sväng-/knappinstruktioner**, inklusive gamla instruktioner, tills rätt spår och riktning åter är säkert identifierade. Samma princip gäller oklara eller icke verifierade vägval.

## 3. Begrepp

| Begrepp | Betydelse |
|---|---|
| **Kontrollpunkt** | Intern teknisk referens till en station eller ett rödljus; ingen ytterligare platstyp behöver visas för spelaren. |
| **Zon** | Ett tredimensionellt område bestående av en eller flera boxar. |
| **Station** | Namngiven zon markerad med två hörn. **Alla rälslinjer inom zonen tillhör stationen**, utan plattformar eller krav på att välja ett särskilt stationsspår. |
| **Rödljus (redlight)** | Kontrollpunkt där spår möts och vissa övergångar kan väljas. |
| **Port** | En fysisk in- eller utgång där ett spår passerar kontrollpunktens gräns. |
| **Spårsektion** | Fysisk korridor mellan två närliggande kontrollpunkter. |
| **Fysiskt spår** | En inspelad rälsgeometri inom en sektion. En sektion kan innehålla flera parallella spår. |
| **Passage** | Användning av ett fysiskt spår i en tillåten riktning. |
| **Övergång** | Tillåten koppling från en inkommande passage till en utgående passage vid en kontrollpunkt. |
| **Namnlager** | Ett valfritt namn som läggs över hela eller delar av ett eller flera spår, exempelvis Riksettan eller Centralbron. |
| **Resväg** | Den väg GPS:en beräknar som en följd av passager och övergångar. |
| **Inspelning/survey** | En inspelningsbehörig användares observerade minecart-färd som används för att registrera fysisk geometri och genomförda övergångar. |

## 4. Systemarkitektur

### 4.1 Serverdel

Servern ansvarar för att:

1. Lagra den auktoritativa järnvägsmodellen.
2. Hantera kontrollpunkter, zoner, spårsektioner, fysiska spår, övergångar och namnlager.
3. Spela in minecart-färder för personer med inspelningsbehörighet.
4. Skapa förslag på sektioner, spårpar och observerade övergångar.
5. Validera alla ändringar innan de publiceras.
6. Bygga en riktad navigationsgraf.
7. Distribuera versionsmärkta nätverkssnapshots och ändringar till kompatibla klienter.
8. Separat behörighetskontrollera navigation, inspektion, namngivning, inspelning och administration/publicering.

Serverdelen får inte kräva JourneyMap eller ladda klientklasser på en dedikerad server.

### 4.2 Klientdel

Klienten ansvarar för att:

1. Ta emot och cachelagra serverns aktuella nätverksvy.
2. Låta spelaren söka efter och välja destination.
3. Beräkna eller följa servergodkänd resväg.
4. Matcha minecartens position och riktning mot rätt fysiskt spår.
5. Visa HUD, aktiv resväg och manöverinstruktioner.
6. Visa ett inbyggt visuellt nätverksläge.
7. Erbjuda **primära UI-flöden** för inspektion, namngivning, survey och administration enligt respektive serverrättighet; kommandon är endast kompletterande och ska använda samma servervalidering.
8. Valfritt spegla samma vy i JourneyMap.

Spelare utan Railway GPS ska kunna ansluta och spela normalt. Servern skickar endast modspecifika data till kompatibla klienter.

## 5. Domänmodell

### 5.1 Zoner

En zon är dimensionsbunden och består av en eller flera axis-aligned bounding boxes. Rödljus kan ha en sammansatt zon för att identifiera infart och utfart. **Stationer använder en enkel zon markerad med två hörn; all räls inne i zonen hör till den stationen**. En station kräver inte en unik rälsanknytning, plattformar eller ett stationsområdessystem med delzoner.

```json
{
  "dimension": "minecraft:overworld",
  "shapes": [
    { "min": [10, 70, 30], "max": [28, 78, 55] },
    { "min": [28, 70, 38], "max": [45, 78, 47] }
  ],
  "enterMargin": 0.0,
  "exitMargin": 1.5
}
```

Krav:

- `Y` ingår alltid så att broar, tunnlar och staplade spår kan skiljas åt.
- Inträde och utträde använder hysteres för att undvika fladdrande tillstånd vid zonkanten.
- Flera sammanhängande ticks kan krävas innan inträde eller utträde bekräftas.
- Överlappande kontrollzoner varnas och kräver uttrycklig bekräftelse.
- Zoner kan visualiseras och redigeras på klienten av behöriga administratörer.

### 5.2 Stationer

En **station** är en **namngiven 3D-zon markerad med två motsatta hörn**. **Alla rälslinjer innanför zonen räknas till samma station**, vare sig den omfattar ett spårslut, genomgående räls eller flera parallella spår. Administratören bestämmer zonens storlek genom hörnen. Ingen särskild per-spårstation, plattform, terminal eller underindelning krävs.

Varje station består av ett **serverunikt visningsnamn**, tekniskt ID, dimension och **en tvåhörnsmarkerad 3D-zon**. Den är **inte knuten till en enda utvald fysisk räls**: varje verkligt rälssegment som passerar zonen kan leda till stationen när färdriktningen och färdvägen är verifierade. Ingen ankomstriktning väljs per station; ankomst är tillåten från varje verifierat spår och verifierad riktning som når zonen. Ett oregistrerat eller otillåtet spår blir inte routbart bara för att det ligger i zonen, och stationens zon sammanfogar aldrig separata fysiska spår eller skapar rödljusövergångar. Två stationer får **inte ha samma visningsnamn** någonstans på servern, även om de finns i olika dimensioner. Namn jämförs efter trimning och utan hänsyn till stora/små bokstäver; namnkonflikter vid skapande, ändring eller import avvisas.

**Endast stationer i spelarens aktuella dimension är valbara destinationer**, även om nätverket innehåller stationer i andra dimensioner. GPS:en får beräkna ankomst till samma stationsstopp från **endera verifierade riktningen**, utan stationstyp, plattformsval eller separat ankomstriktningskonfiguration. Spelaren kan välja destination **innan ombordstigning**; valet är då väntande och ingen aktiv rutt eller svänginstruktion visas. Om spelaren sitter i en **stillastående** minecart på registrerad räls förblir stationsvalet väntande: eventuellt kan status **”Väntar på färdriktning”** visas, men ingen aktiv rutt eller sväng-/knappinstruktion. **Först när vagnen börjar röra sig med säkert, stabilt observerad riktning** väljer GPS:en automatiskt kortaste giltiga rutt från faktisk rälsposition och riktning och aktiverar vägledningen. Om färdriktning eller rätt registrerat spår inte kan avgöras ska GPS:en inte gissa. GPS:en markerar **Framme** så snart minecarten med säker färdhistorik **kommer in i den valda stationszonen längs en giltig verifierad färdväg**, **oavsett vilket av stationens registrerade spår** vagnen använder; ett fullständigt stopp eller låg hastighet krävs inte. När **Framme** väl har bekräftats ska texten ligga kvar även om vagnen fortsätter röra sig. **Vid all avstigning, även före ankomst, avslutas aktiv navigation omedelbart och navigations-HUD:en försvinner.** Ny resa kräver nytt destinationsval. En förbipassage **utanför zonen** eller på en färdväg som inte är verifierat giltig får inte utlösa ankomst. **Ett annat spår innanför samma stationszon är däremot inte ett 'fel spår'.**

### 5.3 Rödljus (redlight)

En rödljus består av:

- tekniskt ID;
- valfritt visningsnamn;
- en kontrollzon;
- portar där spår går in i eller ut ur zonen;
- uttryckligen tillåtna övergångar;
- valfria instruktioner för knappar eller andra kontroller.

Kontrollzonen omsluter hela området där vägvalet sker. Den används för att dela inspelningar, identifiera inträde och utträde och bekräfta vilket vägval vagnen faktiskt genomförde.

Zonen bestämmer inte automatiskt vilka svängar som är möjliga. Det gör övergångarna. **Rödljus är black boxes:** spår spelas bara in fram till/ifrån zongränsens portar; interna rälsbitar, växelmekanik och redstone modelleras inte. En inspelad och säkerställd infart→utfart kan registreras som giltig övergång utan separat manuell publiceringsbekräftelse. Oklara vägval förblir overifierade tills de kan verifieras eller administratören bekräftar dem. Stationer är de enda stopp-/ankomstobjekten; rödljus är enbart vägval.

### 5.4 Spårsektioner och fysiska spår

En spårsektion förbinder två närliggande kontrollpunkter och grupperar de fysiska spår som hör till samma korridor.

```text
Spårsektion: västra infarten
├── spår A: väst → öst
├── spår B: öst → väst
└── eventuellt reservspår
```

Varje fysiskt spår innehåller:

- stabilt tekniskt ID;
- sektionstillhörighet;
- två portar/ändpunkter;
- inspelad 3D-geometri;
- faktisk längd;
- tillåtna riktningar;
- status, exempelvis utkast, verifierat eller avstängt.

Följande måste stödjas:

- **Tvåspårigt:** separat geometri för vardera riktningen.
- **Enkelspårigt dubbelriktat:** samma geometri kan traverseras i båda riktningarna **först när vardera tillåten färdriktning är dokumenterad genom inspelning eller uttryckligen godkänd av en administratör**. En enda inspelad resa verifierar bara den faktiskt observerade riktningen.
- **Enkelriktat:** endast en passage är tillåten.
- **Fler än två spår:** exempelvis extra eller parallella fysiska rälslinjer, utan krav på plattformar.

Två parallella spår ska inte representeras genom att samma linje vänds baklänges om de i världen ligger på olika räls. **Det ska aldrig härledas att motriktad passage är tillåten enbart ur ett spårs geometri, två ändpunkter eller tillgången till en framåtriktad passage.** En motriktad inspelning kan verifiera motsatt passage på **samma fysiska räls** utan att duplicera geometrin. Administratören får också explicit verifiera den motsatta riktningen, med serverkontroll och loggad åtgärd. Detta är **inte** samma sak som att para ihop två separat liggande parallella rälslinjer, vilket fortfarande kräver särskilt administratörsgodkännande.

### 5.5 Passager och övergångar

En passage är ett fysiskt spår plus en riktning. Ruttgrafen arbetar med passager, inte bara med sektioner. **En passage får ingå i ruttgrafen endast om just dess färdriktning är verifierad genom inspelning eller uttryckligt administratörsgodkännande.** Samma fysiska spår kan användas åt båda hållen, men det räcker aldrig att motsatt riktning *teoretiskt* verkar möjlig. Motsatt riktning är en separat verifiering och får inte automatiskt aktiveras av att framriktningen registrerats.

En övergång beskriver ett verifierat vägval:

```text
inkommande passage → kontrollpunkt → utgående passage
```

Övergången kan innehålla:

- tillåten/avstängd status;
- verifierad identifiering av infarts- och utfartsport, **utan att lagra intern rälsgeometri**;
- kontroll-/knappinstruktion;
- ordagrann administratörsskriven instruktion (inte automatiskt översatt);
- källa, exempelvis observerad inspelning eller administratörsbeslut.

Att två spår möter samma kontrollzon betyder inte att övergången mellan dem automatiskt är möjlig.

### 5.6 Namnlager

Namnlager är presentation ovanpå den fysiska modellen. Ett namnlager kan omfatta hela eller partiella intervall av flera spår.

**Namngivning ska vara enkel:** den behöriga användaren markerar en bit befintlig räls och ger den ett namn. Om namn ska synas i HUD räcker det att kunna ange **huvudled** (exempelvis Riksettan) eller **lokalt namn** (exempelvis Centralbron). Tekniska typer såsom `way`, `bridge` och `tunnel` får fortsätta finnas internt för kompatibilitet och valfri kartstyling, men **ingen lista av avancerade objektkategorier ska krävas i namngivningsflödet**. Namn är inte stationer, rödljus eller nya vägval.

Ett namnlager innehåller:

- tekniskt ID;
- visningsnamn;
- typ;
- prioritet;
- valfri färg och ikon;
- en mängd spårintervall.

```json
{
  "id": "centralbron",
  "name": "Centralbron",
  "type": "bridge",
  "priority": 50,
  "ranges": [
    { "track": "track_eastbound", "from": 42.0, "to": 118.0 },
    { "track": "track_westbound", "from": 40.0, "to": 116.0 }
  ]
}
```

Konsekvenser:

- Centralbron kan börja och sluta vid visuella markeringar mitt på ett spår utan nya grafnoder.
- Riksettan kan fortsätta genom flera spårsektioner och rödljus.
- Samma fysiska spår kan tillhöra flera namn, exempelvis både Riksettan och Centralbron.
- Namnlösa spår fungerar fullt ut men ger ingen påhittad etikett i HUD.
- Namn påverkar inte vilka resvägar som är fysiskt möjliga.

### 5.7 Resväg

En resväg lagras inte som en duplicerad helhetsgeometri. Den beräknas som en följd av passager och övergångar genom det gemensamma fysiska nätet.

Exempel:

```text
Station A
→ namnlös anslutning
→ Riksettan
→ Riksettan · Centralbron
→ Riksettan
→ stationsinfart
→ Station B
```

Flera resor som passerar Centralbron använder samma registrerade spår och samma namnlager. Bron behöver inte spelas in på nytt för varje destinationskombination.

## 6. Serverlagring

Den auktoritativa modellen bör lagras transaktionellt med schemaversion, exempelvis i en världslokal SQLite-databas:

```text
<världsmapp>/railwaygps/network.db
```

Rekommenderade logiska tabeller/samlingar:

```text
control_points
zones
zone_boxes
stations
redlights
ports
track_sections
physical_tracks
track_geometry
allowed_transitions
custom_instructions
labels
label_track_ranges
network_revisions
```

Krav:

- Ändringar sker i transaktioner och blir aktiva först efter fullständig validering.
- Den senast fungerande revisionen behålls om en ändring misslyckas.
- Automatiska säkerhetskopior skapas före destruktiva eller större operationer.
- JSON-export och JSON-import ska finnas för granskning, backup och externa verktyg.
- Importerad data aktiveras aldrig innan hela modellen validerats, **inklusive kontroll av serverövergripande unika stationsvisningsnamn**. Kolliderande namn avvisas utan automatisk omdöpning.
- ID:n valideras och användarinmatning får inte kunna skriva utanför världens Railway GPS-data.
- Databasen är en implementationdetalj; klienter får en kompakt nätverksmodell, inte direkt databasåtkomst.

## 7. Administration och redigering

Behörigheter ska vara **separata och serverkontrollerade**. Vanlig navigation kräver ingen redigeringsroll. Fyra oberoende rättigheter används:

| Rättighet | Tillåten åtgärd |
|---|---|
| **Inspektion** | Se teknisk nätverksinformation, status och felsökningsverktyg; inte ändra data. |
| **Namngivning** | Föreslå/skapa/ändra namnlager på befintliga verifierade spår; inte ändra fysisk topologi. |
| **Inspelning** | Starta/avsluta/avbryta minecart-surveys; giltigt `finish` publicerar automatiskt det inspelade nätverkstillskottet enligt serverns policy. |
| **Administration/publicering** | Skapa/ändra kontrollpunkter, portar, spår, tillåtna övergångar och rättigheter; bekräfta spårpar, importera/återställa revisioner och utföra destruktiva ingrepp. |

**In-game-UI är det huvudsakliga gränssnittet för alla fyra behörigheterna.** En behörig användare ska se relevanta åtgärder från Railway GPS-menyn och utföra dem med begripliga knappar, sökning, tvåhörns-/sträckmarkeringsverktyg och tydliga förhandsvisningar. Inspektör, namnredaktör och survey-användare får egna ingångar **utan att behöva bli administratörer**. Kommandon är alternativa administrativa/tekniska hjälpmedel, **inte förutsättningar för att använda moddet**. Samma serveråtgärder, behörighetskontroll, validering och atomiska publiceringsregler gäller oavsett om de utlöses från UI, API eller kommando.

Servern kontrollerar varje begäran, även via klient-GUI och API. Klienterna får bara data de har rätt att se. Rollernas exakta kommandon/behörighetsnoder är en teknisk detalj, men separationen är beslutad. En användare kan ha flera rättigheter; en ren inspelare behöver inte moderatorbehörighet. Spårparning kräver uttryckligt godkännande med administrations-/publiceringsrätt.

Vanliga spelare ska bara kunna söka fram och välja **stationer i den dimension där spelaren för tillfället befinner sig** som destinationer, oavsett möjliga förbindelser till andra dimensioner. Spårslut hanteras som vanliga stationer, inte som en extra terminaltyp. **Unika stationsnamn gäller ändå på hela servern**, även mellan dimensioner. Vid destinationsval som görs via klient eller kommando måste dimensionskontrollen vara densamma; admininspektion av andra dimensioner är en separat behörighetsstyrd funktion.

### 7.1 Visuellt markeringsverktyg

Administratören ska kunna markera två hörn i världen med ett verktyg. Klienten visar omedelbart:

- valda hörn;
- transparent volym och kantlinjer;
- zonens storlek;
- spår som träffas av zonen;
- överlapp med befintliga zoner;
- varningar innan zonen bekräftas.

Flera boxar kan läggas till samma zon. JourneyMap eller nätverksinspektören kan användas som kompletterande redigeringsyta.

### 7.2 Skapa ett rödljus

**Huvudflöde i UI:** öppna Railway GPS (`R` som standard), välj **Adminverktyg → Rödljus → Skapa**; genomför hela flödet med dialoger och visuellt tvåhörnsverktyg. **Kommandon behövs inte.**

Arbetsflödet är:

1. Skapa rödljusobjektet.
2. Markera kontrollzonen runt hela växel-/rödljusområdet.
3. Spela in spåren som passerar zonen.
4. Låt moddet föreslå portar vid zonens gräns.
5. Verifiera observerade möjliga övergångar (administratörsbeslut behövs om observationen är osäker).
6. Lägg vid behov till kontrollinstruktioner.

**Sekundärt exempel för tekniskt bruk**, inte ordningen i det normala UI-flödet:

```mcfunction
/rgps redlight create sigma
/rgps redlight zone select sigma
/rgps zone add-box
/rgps zone confirm
```

### 7.3 Skapa en station

**Huvudflöde i UI:t:** öppna **Railway GPS → Adminverktyg → Skapa station**.

1. Markera **två motsatta hörn** så att den tänkta stationens räls täcks av en gemensam **3D-zon**.
2. Ange ett **serverunikt stationsnamn**.
3. Förhandsvisa området och spara. **All räls som ligger innanför zonen hör till stationen**; det får finnas flera parallella eller genomgående spår. Ingen dialog för att välja ett visst spår behövs.

Validera att zonen är giltig och stationsnamnet unikt. **Flera rälslinjer i samma stationszon är uttryckligen tillåtet, inte tvetydighet eller fel.** Den befintliga fysiska ruttgrafens verifierade riktningar används fortfarande: en zon gör **inte** separata spår förbundna med varandra. Inga plattformar, terminalobjekt eller separata stationsdelar skapas.

### 7.4 Spela in fysisk räls

En användare med **inspelningsbehörighet** får via **Railway GPS → Inspelning** använda UI-knapparna **Starta, Status, Avsluta/publicera och Avbryt**. Den får starta en lång survey **var som helst på verklig räls**, även mellan stationer och rödljus och utanför registrerade zoner. Start i en kontrollpunktszon krävs **inte**. Inspelningen får passera flera rödljus utan omstart. **Före publicering vid `finish` måste systemet säkert kunna koppla hela inspelningen till det registrerade nätet**, inklusive start-/slutanslutningar och rätt fysisk räls/riktning. Om det inte går ska hela inspelningen avvisas, utan att skapa påhittade kontrollpunkter.

**Följande kommandon är sekundära alternativ till UI-knapparna**, inte krav på operatörsbehörighet eller på kommandokunskap:

```mcfunction
/rgps survey start
/rgps survey status
/rgps survey finish
/rgps survey cancel
```

Vid inspelning registreras **fysisk räls utanför rödljusens zoner**. Inspelning som startar eller slutar mitt på en tidigare känd spårsträcka får matchas till just den rälsen utan att skapa extra platsobjekt eller duplicerad geometri. Vid zongränsen identifieras infartsporten, intern rälsgeometri ignoreras och nästa inspelade fysiska spår börjar vid utfartsporten. Observerade och entydigt verifierade infart→utfart-övergångar kan ingå i samma publikation. Stationer är enkla zoner som kan innehålla **flera spår**; stationszoner får inte behandlas som redlight-black-boxes eller tvinga val av ett enda stationsspår.

**Beslutad publiceringsregel:** **UI-knappen Avsluta/publicera** (och motsvarande sekundära `/rgps survey finish`) analyserar och validerar **hela inspelningen** och publicerar alla giltiga tillskott **direkt, atomiskt och i en revision**, utan ett obligatoriskt separat `confirm`-/`discard`-steg. Vid kritiskt fel, osäker koppling som kräver manuell kontroll eller misslyckad skrivning publiceras **ingenting**; användaren får en begriplig felrapport. Survey-avbrytning får inte spara delvis data. Visuell förhandsvisning och diagnoser är frivilligt stöd, inte ett extra publiceringskrav. Administratörsåtgärder för senare redigering/återställning är en separat process.

**Spårpar är ett undantag från automatiken:** vid `finish` får moddet föreslå geometriskt närliggande motriktade spårpar men får **aldrig para ihop dem automatiskt**, även om exakt en kandidat hittas. Ett förslag kräver efterföljande uttryckligt godkännande av person med behörigheten administration/publicering. Inspelade spår kan ändå publiceras oparade om de är giltiga. När redan existerande spår upptäcks får systemet inte destruktivt slå samman dem automatiskt.

### 7.5 Tvåspåriga sträckor

Vardera fysiska rälsen spelas in separat. **Godkännande av motsatt färdriktning på en och samma räls är inte parning av två skilda spår.** Moddet **föreslår par utifrån fysisk närhet, riktning och geometrisk överensstämmelse**; det räcker inte att spåren har omvända kontrollpunkter. Förslaget visar vilka två spår det gäller och osäkerhet. **Ingen parning sker automatiskt**, inte ens med en enda kandidat. En person med administrations-/publiceringsrätt måste godkänna paret uttryckligen. Oparade, giltiga spår fungerar i nätet.

Parningen påverkar presentation och administration. Ruttberäkningen fortsätter att använda respektive riktad passage.

### 7.6 Namnge en känd sträcka mellan två punkter

**Huvudflödet öppnas i Railway GPS → Namnge räls med namngivningsbehörighet.** Detta är det enda normala arbetsflödet för namngivning: en användare med namngivningsbehörighet markerar **sträckans början och slut på redan registrerad räls**, anger ett namn och bekräftar den visuella förhandsvisningen. Ingen separat minecart-färd eller namninspelningsfunktion krävs. Om den fysiska rälsen ännu inte är registrerad måste den först registreras med den separata vanliga survey-funktionen.

Arbetsflödet är:

1. Ange **visningsnamn** och välj vid behov **huvudled** eller **lokalt namn**. Moddet genererar tekniskt ID automatiskt; användaren ska inte behöva förstå eller mata in det i det vanliga gränssnittet.
2. Markera en startpunkt och en slutpunkt i världen eller på kartan.
3. Moddet projicerar punkterna på närmaste kompatibla registrerade spår.
4. Moddet räknar fram sammanhängande kandidater genom det kända spårnätet.
5. Kandidaten förhandsvisas tydligt i världen och på kartan.
6. Om den markerade rälsen har ett **separat parallellt spår som redan administratörsgodkänts som par**, visa ett enkelt val: **”Namnge även det parade spåret? Ja/Nej”** med **Nej som förvalt**. Utan godkänt par visas inte valet. Förhandsvisa vilka fysiska spår och delintervall som faktiskt namnges.
7. Namnet sparas först efter uttrycklig bekräftelse. Ett Ja ändrar **bara namnlager**, inte parstatus eller tillåtna riktningar; namngivningsbehörighet är tillräcklig för redan godkänt par.

Första och sista spåret får partiella intervall. Namngivningen skapar därför inga kontrollpunkter och förändrar inte ruttgrafen.

**Primärflöde:** i UI:t anger namn, markerar start/slut, granskar möjliga fysiska sträckor och bekräftar. **Kommandon är enbart illustrativa sekundära alternativ:**

```mcfunction
/rgps label select start way riksettan Riksettan
# Klicka startpunkt och slutpunkt med markeringsverktyget
# Om godkänt parallellspår finns: välj Ja/Nej i förhandsvisningen (Nej är standard)
/rgps label confirm
```

Om endast en sammanhängande väg finns mellan punkterna kan den väljas automatiskt för **förhandsvisning**. Om flera vägar är möjliga måste moddet visa alternativen och kräva ett val; ett valfritt mellanpunktverktyg kan hjälpa till att skilja dem åt utan att skapa nya objekt. Namnet publiceras först efter att den avsedda rälssträckan förhandsvisats och bekräftats. En namngivning får aldrig tyst välja en godtycklig gren eller en närliggande parallell räls. **Ett godkänt parallellspår är aldrig automatiskt med i namngivningen**; det krävs ett uttryckligt Ja.

**Skapa ingen separat funktion för att spela in en namngiven färd med minecart.** En inspelning används bara för att kartlägga fysisk räls; namn läggs därefter på med startpunkt, slutpunkt och namn.

### 7.7 Namnge Riksettan

En behörig användare markerar **början och slutet på den registrerade rälssträckan**, anger `Riksettan`, granskar den föreslagna sammanhängande vägen och bekräftar. Om två valbara förgreningar finns väljer användaren den korrekta utpekade vägen. En längre led kan därmed fortsätta genom flera rödljus utan att spela in någon separat namnresa. Om ett separat parallellt spår **redan är godkänt som par av administratör** kan användaren välja **Ja** i ett enkelt Ja/Nej-val (förvalt: **Nej**) för att inkludera det i förhandsvisningen.

### 7.8 Namnge Centralbron

Användaren markerar **början och slutet** på den redan registrerade rälsen, exempelvis två markeringar vid brons ändar, anger `Centralbron` och bekräftar den exakta visuella sträckan. Om ett separat parallellt spår **redan är administratörsgodkänt som par**, kan användaren svara **Ja** för att inkludera motsvarande del av spåret. **Nej är standard**; inget val visas utan godkänt par. Ingen namninspelning genom körning och inga falska rödljusnoder behövs.

### 7.9 Anpassade instruktioner

En instruktion kopplas till en utgång eller en specifik inkommande–utgående övergång vid en kontrollpunkt.

```text
Kontrollpunkt: Sigma
Utgående passage: Martyrögrenen
Instruktion: Välj knappen för Martyrön
```

En generell instruktion för utgången kan kompletteras med mer specifika övergångsregler. Den mest specifika giltiga regeln vinner. **Administratörsskrivna anpassade instruktioner visas ordagrant**, på exakt det språk och med den text administratören angav, **oavsett spelarens språk**. Ingen översättningsvariant krävs. Vid **Osäker position** visas inte denna eller andra sväng-/knappinstruktioner.

## 8. Tekniskt inspelningsbeteende

Inspelningen sker på serversidan och kopplas till den **inspelningsbehöriga** användaren och den specifika minecart som används vid start.

### 8.1 Insamling

- Sampla minecartens position och rörelse under server-ticks.
- Identifiera rälsblocket som vagnen följer när detta kan göras säkert.
- Registrera ordning, kurvor, lutningar och dimension.
- Undvik duplicerade punkter när vagnen står stilla.
- Registrera passage genom kontrollzoner med tidsordning och riktning. Rödljusens inre är **opaque**: logga infarts-/utfartsport och observerat vägval, inte intern rälsgeometri.
- Tillåt **survey-start var som helst på verklig räls**, även utan registrerad zon vid start. Kontrollera säkert mot nätets befintliga fysiska spår före publicering.
- Visa den inspelade linjen live på administratörens klient.

### 8.2 Efterbearbetning

- Beräkna faktisk färdlängd längs spåret.
- Förenkla raka delar utan att förlora kurvor, höjdskillnader eller parallell separation.
- Projektera kontrollzonernas gränspassager på geometrin.
- Identifiera befintlig geometri som den nya inspelningen sannolikt motsvarar, **även för en start mitt på ett spår**; saknas säker anslutning till nätet får hela inspelningen inte publiceras.
- Skapa förslag, aldrig tysta destruktiva sammanslagningar eller automatisk spårparning. Entydigt giltiga inspelningar publiceras vid `finish` utan manuellt publiceringssteg; osäkra inspelningar avvisas helt i stället för att aktiveras delvis.

### 8.3 Strikt avbrott

Om den inspelningsbehöriga användaren lämnar minecarten avbryts inspelningen omedelbart och all tillfällig data kastas. Samma regel gäller vid:

- frånkoppling;
- död;
- dimensionsbyte;
- förstörd minecart;
- byte till en annan minecart.

Manuell avbrytning:

```mcfunction
/rgps survey cancel
```

Att nå en station eller kontrollzon får ge ett förslag om att avsluta, men ska inte automatiskt spara eller publicera inspelningen.

## 9. Navigationsgraf och ruttberäkning

Grafens tillstånd bör vara mer detaljerat än en nod per rödljus. Ett lämpligt internt tillstånd är en port eller spårände vid en kontrollpunkt.

Grafkanter är:

1. **Passagekanter:** färd längs ett fysiskt spår i tillåten riktning.
2. **Övergångskanter:** verifierad rörelse genom en kontrollpunkt från en passage till en annan.

Detta förhindrar att alla spår vid samma rödljus automatiskt betraktas som ömsesidigt anslutna.

Ruttberäkningen väljer **automatiskt den kortaste giltiga vägen** efter **återstående längd på verifierad fysisk räls utanför rödljuszoner**, från spelarens verkliga position och **säkert observerade färdriktning sedan vagnen börjat röra sig**. En stillastående vagn ger inget säkert färdriktningsval och ska inte starta en rutt av sig själv. På det första spåret räknas **bara den del som återstår framåt**, inte spårets hela längd. Endast spår i tillåten riktning och uttryckligen verifierade rödljusövergångar får användas. Ett rödljusbidrag är **noll**: ingen uppskattad väntetid, manuell rödljuskostnad, övergångsvikt eller extra straffkostnad behövs. Namngivna vägar prioriteras **inte** framför kortare rutter. Avstängda spår och övergångar är **otillåtna**, inte dyrare alternativ. Spelaren behöver **inte** välja mellan flera förslag; likvärdiga kortaste rutter kan avgöras deterministiskt utan extra meny.

Dijkstra eller A* kan användas. Ruttresultatet är en följd av specifika riktade passager och övergångar, vilket gör att klienten vet exakt vilket av två parallella spår som avses.

## 10. Positionsbestämning

Klienten gör kontinuerlig map matching med:

- spelarens/minecartens 3D-position;
- färdriktning och hastighet (**observera verklig, stabil rörelse innan aktiv rutt startar; stående minecart får inte ge ett gissat startval**);
- föregående bekräftade passage;
- planerad nästa passage;
- kontrollzonernas inträde och utträde;
- avstånd längs kandidaternas geometri.

Färdriktning och färdhistorik ska väga tungt så att två parallella spår inte sammanblandas.

Tillstånd kan vara:

```text
På bekräftat spår
→ Närmar sig kontrollpunkt
→ Inne i kontrollzon
→ Utgående spår observerat
→ Ny passage bekräftad
```

Spelaren kan **välja en annan station i sin aktuella dimension under aktiv resa**. GPS:en ersätter då direkt destinationsvalet och beräknar **kortaste verifierade giltiga rutt från säkert matchad aktuell rälsposition och riktning**, utan att kräva avslut/ny ombordstigning. Om rälspositionen är osäker hålls den nya stationen vald utan gissad manöver, tills säkert underlag finns. Om spelaren tar en annan verifierad övergång än den planerade räknas automatiskt den **kortaste återstående giltiga rutten** om från bekräftad aktuell rälsposition. **När aktiv rutt faktiskt ersätts av en annan giltig rutt**, visa kort **”Rutt omräknad”** och sedan den nya manöverinstruktionen. Notisen är **icke-blockerande**, kräver ingen bekräftelse och får inte felaktigt visas som framgång om ingen rutt finns. Om den faktiska rutten är oförändrad behövs ingen notis. Om **den valda destinationsstationen har tagits bort från en fullständigt validerad ny nätverksrevision**, ska navigationen **avslutas direkt**, aktiva instruktioner/ruttlinjer rensas och spelaren få en **separat tydlig notis ”Stationen har tagits bort”**. Detta är **inte** ”Ingen giltig väg” och får varken behålla den borttagna stationen som väntande mål eller välja en annan station automatiskt. Om **ingen giltig rutt** går att beräkna till det aktuellt valda **befintliga** resmålet (även efter att vagnen börjat röra sig eller efter destinationsbyte) visas **”Ingen giltig väg till stationen”**, och resmålet **behålls** utan vilseledande gamla sväng-/knappinstruktioner eller avstånd. GPS:en gör ett **automatiskt nytt försök när relevant verifierad position, fysisk rälsriktning eller nätverksrevision/övergångsstatus förändras**, inte vid varje tick; hittar den en giltig väg aktiveras normal vägledning utan knapptryck. Om läget är tvetydigt visas **destinationen och Osäker position**, medan alla vägvals-/sväng-/knappinstruktioner döljs tills rätt spår kan identifieras. **Vid avstigning upphör aktiv navigering direkt, även före ankomst.** Efter bekräftad ankomst visas **Framme** fram till avstigning; då avslutas navigeringen.

## 11. Navigationsinstruktioner och HUD

HUD:en ska prioritera handling framför tekniska detaljer. Vid stillastående minecart med förvald station visas **ingen aktiv rutt och inga körinstruktioner**; en diskret status **”Väntar på färdriktning”** kan visas. Vid en **faktiskt genomförd omräkning till en ny giltig rutt** visas kort den lokaliserade notisen **”Rutt omräknad”**, därefter den nya instruktionen utan kvittering. Om ingen väg finns visas endast det vanliga tydliga felet.  **Som standard visas endast HUD:en**, inte en linje över rälsen i Minecraft-världen. Vid aktiv, säkert matchad resa ska HUD:en visa **två separata avstånd**: (1) återstående körsträcka längs rutten till **nästa rödljus**, och (2) återstående körsträcka längs hela rutten till **destinationsstationen**, båda i block. Nästa rödljus avser bara rödljus **på den planerade återstående rutten**, inte närmaste rödljus fågelvägen. Om resan saknar ytterligare rödljus visas exempelvis ”Inget fler rödljus” i stället för en påhittad distans. Inne i rödljuszonen visas att spelaren är i rödljuset i stället för att beräkna en intern genomfartssträcka (som inte är modellerad). **Vid osäker position gäller tidigare säkerhetsbeslut**: visa destination och ”Osäker position” utan obekräftade avstånd eller sväng-/knappinstruktioner.

```text
RAILWAY GPS · Central
RIKSETTAN · Centralbron

Välj knappen för Martyrön
Nästa rödljus: Sigma · 28 block

Återstående till station: 1 240 block
```

### 11.1 Automatisk manöver

Den sista stabila vektorn på inkommande spår jämförs med den första stabila vektorn på utgående spår. Resultatet klassificeras som:

- rakt fram;
- vänster;
- höger;
- tillbaka;
- osäkert.

Geometrin ska hämtas från den verkliga inspelade rälsen **utanför rödljusets black-box-zon**, inte från raka linjer mellan kontrollpunkternas mittpunkter.

**Visningstidpunkt:** välj rätt vägvals-/knappinstruktion för **nästa rödljus** när spelaren på sin aktuella bekräftade räls **börjar närma sig** rödljuset (baserat på återstående spåravstånd, med en konsekvent justerbar förvarningsgräns). Visa den inte för tidigt längs hela linjen eller först när vagnen redan har gått in i zonen. **Behåll samma relevanta instruktion inne i rödljusets zon** fram tills faktiskt utgående spår har bekräftats. Vid **Osäker position** ska instruktionen döljas även om rödljuset är nära eller vagnen är inne i zonen. Exakta avståndsvärden är en teknisk inställning, inte ett krav på användarval.

### 11.2 Instruktionsprioritet

1. Specifik manuell instruktion för exakt övergång.
2. Generell kontrollinstruktion för den valda utgången.
3. Automatisk vänster/höger/rakt/tillbaka-instruktion.
4. Neutral instruktion mot nästa relevanta mål **bara när rätt spår är känt** men manöverns riktning inte kan avgöras. Vid **Osäker position** visas **ingen sväng-, vägvals- eller knappinstruktion**, enbart destinationen och osäkerhetsstatus.

### 11.3 Namnpresentation

- HUD visar **högst en huvudled och högst ett lokalt namn** (t.ex. bro/tunnel) samtidigt: `Riksettan · Centralbron`. Huvudled är primärt `way` och lokalt namn en närliggande namngiven konstruktion; vid flera kandidater väljs högsta namnlagerprioritet inom respektive kategori. Saknas en kategori visas enbart den andra. Övriga namn kan visas i karta/inspektör.
- Om spelaren fortsätter inom samma led kan HUD visa `Fortsätt på Riksettan`.
- Vid namnbyte kan den visa `Fortsätt åt vänster in på Hamnleden`.
- Namnlösa anslutningar visar inget internt ID; HUD använder nästa station eller kontrollpunkt som kontext.
- Alla **inbyggda standardtexter** ska vara lokaliserbara. **Administratörsskrivna instruktioner visas exakt som de skrevs** oavsett spelarens språk.

## 12. Inbyggd visuell nätverksvy

Den visuella nätverksinspektören är en kärnfunktion och ska fungera utan JourneyMap.

### 12.1 Visningslägen

**Vid första användningen är 3D-rutten avstängd; då visas bara navigations-HUD.** Spelaren kan med en enkel inställning eller tangent **slå på/av endast den planerade aktiva rutten ovanför rälsen**, utan krav på JourneyMap. **Spara spelarens val i en lokal beständig klientinställning**, så att det överlever **nya resor och omstarter**. Spelarens val gäller vid nästa giltiga aktiva resa utan att fråga igen; att navigeringen avslutas tar bort den aktuella ritade rutten men **återställer inte spelarens val till Av**. Inbyggd HUD visas fortsatt oberoende av värdet. Den aktiva ruttlinjen ska följa **verklig inspelad extern räls**, aldrig hittad på geometri inne i rödljusens black boxes, och den ska uppdateras vid omplanering samt försvinna vid avstigning/avslutad navigering.

Den **separata, valfria nätverksinspektören** kan fortsätta ha lägen för att inspektera data utan att ändra spelarens standardläge:

- **Av (första användningen):** endast navigations-HUD; senare används spelarens senast sparade visningsval.
- **Aktiv rutt:** frivillig 3D-markering av **endast den planerade rutten** ovanför rälsen.
- **Nätverk:** inspektera spår, stationer, rödljus och namn enligt läsbehörighet.
- **Administration:** zoner, portar, riktningar, övergångar och valideringsfel för behöriga administratörer.

Inspektions-/administrationsverktyg kräver respektive redan beslutade serverbehörighet; spelaren ska inte behöva slå på hela nätverksoverlayen för att se sin egen rutt.

### 12.2 Världsrendering

Klienten ska kunna visa:

- stationernas enkla **tvåhörnszoner**, inklusive all registrerad räls som passerar inom dem;
- rödljusens kontrollzoner;
- inspelade spår som linjer ovanför rälsen;
- riktningspilar;
- tillåtna övergångar genom rödljus;
- namnlager och deras exakta intervall;
- aktiv resväg och valt fysiskt spår;
- pågående inspelning;
- utkast och felaktig geometri.

Föreslagna standarder:

| Objekt | Standardvisning |
|---|---|
| Namnlöst spår | Grå linje |
| Namngiven led | Ledens färg |
| Aktiv resväg | Kraftig blå linje |
| Station | Cyan zon |
| Stationens stoppunkt | Grön markering |
| Rödljus | Orange zon |
| Otillåten övergång | Rött kryss |
| Fel eller lös geometri | Röd markerad linje |
| Pågående inspelning | Gul linje |

Färger, opacitet och linjemönster ska kunna anpassas och inte vara den enda informationsbäraren.

### 12.3 Nätverksutforskare

Klienten ska ha en sökbar panel:

```text
Nätverk
├── Leder
│   └── Riksettan
├── Konstruktioner
│   └── Centralbron
├── Stationer
│   └── Centralstationen
├── Rödljus
│   └── Sigma
└── Namnlösa spår
```

När ett objekt väljs ska det kunna:

- markeras i världen och på kartan;
- centreras i en kartvy;
- visa anslutna objekt;
- visa spår och riktningar;
- visa tillåtna övergångar och instruktioner;
- visa datakälla, revision och valideringsstatus;
- visa tekniska ID:n endast i administratörsläge.

### 12.4 Visuell inspelning och redigering

**Primär interaktion, fastställd i commit [`821880e`](https://github.com/Crispy-Monke-eSports/Railway-GPS/commit/821880e7f3909d3fee1bbf84063b51e3c949f703):** Railway GPS öppnas med en konfigurerbar tangent (`R` som standard). Det **SimpleBlueprints-inspirerade UI-systemet**, med sökbara skärmar, behörighetsstyrda verktygsknappar, direkta åtgärder, visuell världmarkering och status-HUD, ska vara **huvudgränssnitt för spelare och administratörer**. Ingen regelbunden åtgärd får endast finnas via `/rgps`-kommando; sådana kommandon är sekundära alternativ för felsökning och avancerad användning och får inte erbjuda andra behörighets-/säkerhetsvägar. Vanliga spelare får en lättåtkomlig inställning för **visa/dölj planerad 3D-rutt**, med **av första gången** och sedan spelarens **lokalt sparade senaste val över både resor och spelstarter**, medan HUD alltid finns kvar vid aktiv navigering. De får en sökbar
destinationslista med **enbart stationer i spelarens aktuella dimension** (spårslut ingår som stationer; rödljus och stationer i andra dimensioner är aldrig valbara destinationer) samt tydliga knappar för att
välja eller **byta destination under pågående resa** eller avsluta aktiv/väntande navigation. Tekniska ID:n behöver inte användas. **Destinationsval till fots sparas som väntande val; aktiv vägledning startar automatiskt först när minecarten verkligen börjar röra sig med säkert matchad registrerad räls och observerad färdriktning.** Vid all avstigning avbryts aktiv navigation, utan automatisk återupptagning.

Behöriga användare får **synliga ingångar i samma Railway GPS-meny** till de **inspektions-, namn-, inspelnings- och administrationsverktyg** som deras separata serverrättigheter tillåter. En inspektör ska kunna söka, filtrera, markera och visa tekniska objekt; en namnredaktör ska kunna göra hela start/slut-förhandsvisa-bekräfta-flödet; en survey-användare ska ha start/status/finish/cancel; en administratör ska kunna skapa/redigera stationer och redlights, kontrollera portar/övergångar, godkänna spårpar och hantera säkra revisioner/import-/exportåtgärder **direkt i UI:t**. Administratörer får dessutom knappen **Adminverktyg**. Den öppnar redigeringsläget, som visar
aktuella musåtgärder och vald geometri i en HUD, i samma anda som verktygspaletten i SimpleBlueprints. För
**stationsstoppzoner och rödljusens kontrollzoner** markerar administratören **första och andra hörnet** med tvåhörnsverktyget; en tredimensionell kontur visas
omedelbart. **Hela stationszonen gäller för alla rälslinjer inom dess gränser**; admin behöver inte välja en enda räls, skapa plattformar eller dela upp stationen. `R` lämnar adminläget. Servern kontrollerar fortfarande behörighet och äger den sparade modellen.

Under inspelning kan geometrin visas live och in- och utträde ur kontrollzoner markeras. Giltig inspelning publiceras **direkt** vid `finish` efter full validering; inga obligatoriska granskningsutkast eller separat `survey confirm` krävs. Geometriska **spårparningsförslag** visas separat och måste bekräftas av behörig administratör innan parning kan sparas.

Vid punktbaserad namngivning ska startpunkt, slutpunkt, eventuella mellanpunkter och hela den beräknade spårvägen visas. Om flera kandidater finns ska användaren kunna växla mellan dem innan bekräftelse. Vald riktning, partiella ändintervall och **endast efter uttryckligt Ja medtagna, redan administratörsgodkända parallellspår** ska gå att särskilja visuellt.

En behörig redaktör ska kunna se vad servern tolkat utan att läsa databas- eller JSON-filer. **Varje UI-flöde ska visa behörighetsfel, valideringsproblem och serverresultat begripligt i samma gränssnitt; det ska aldrig krävas en efterföljande kommandorad för att slutföra, publicera eller förstå resultatet.**

## 13. JourneyMap-integration

JourneyMap är valfri och använder samma presentationsmodell som den inbyggda inspektören. **JourneyMap aktiveras inte av att vanlig navigation startar.** Den inbyggda 3D-världsrutten är **av första gången**, men följer därefter spelarens **sparade på/av-inställning** vid nya resor; HUD visas i båda fallen. JourneyMap och dess lager styrs separat. JourneyMap får fortsätta visa valda kartlager om spelaren uttryckligen använder dem.

Önskade funktioner:

- visa båda fysiska spåren separat;
- visa riktningspilar eller riktad stil;
- visa namngivna leder sammanhängande genom flera kontrollpunkter;
- visa Centralbron endast över dess registrerade intervall;
- visa stationernas enkla stoppunkter eller stoppzoner på rälsen;
- markera den aktiva passagen starkare än övriga spår;
- visa kontrollzoner och fel endast i administrationsläge;
- låta behöriga redaktörer markera två punkter, förhandsgranska spårvägen mellan dem och skapa ett namnlager;
- låta spelaren välja destination via stationer på kartan när API:t tillåter det.

Railway GPS äger alltid grafen, namnen och navigeringen. JourneyMap är endast visning och möjlig inmatning.

## 14. Synkronisering

Föreslaget anslutningsflöde:

1. Servern laddar och validerar senaste aktiva revision.
2. En kompatibel klient annonserar protokollstöd; **servern bestämmer själv** användarens separata inspektions-, namn-, inspelnings- och administrations-/publiceringsrättigheter och filtrerar snapshots därefter.
3. Servern skickar en kompakt snapshot med nätverksversion/hash.
4. Klienten bygger rumsliga index och renderingscache.
5. Senare ändringar skickas som revisionerade deltauppdateringar eller ny snapshot vid behov.

Krav:

- Paket ska storleksbegränsas och vid behov delas upp.
- Klienten aktiverar inte en ofullständig revision.
- När en **fullständigt verifierad** ny nätverksrevision **tar bort vald destinationsstation**, ska aktiv navigering avslutas direkt med meddelandet **”Stationen har tagits bort”**, och både rutt/HUD-vägledning rensas; varken omplanering till annan station eller återförsök mot det borttagna ID:t får ske. I övrigt gäller: när en ny giltig nätverksrevision ändrar, inaktiverar eller tar bort spår eller rödljusövergångar ska en pågående resa **automatiskt beräkna om den kortaste giltiga återstående vägen** från bekräftad aktuell räls/riktning. Om ingen rutt finns, **visa ett tydligt 'Ingen giltig väg till stationen'-fel** och upphör att visa tidigare ogiltiga körinstruktioner. Vid osäker position visa `Osäker position` utan gissad rutt. **Ingen manuell omstart krävs om en alternativ giltig väg finns.** Ankomstläge `Framme` förblir kvar tills avstigning.
- Administrationsmetadata skickas endast till behöriga klienter när den inte behövs för vanlig navigering.
- Servern förblir auktoritativ även om klienten visar eller föreslår redigeringar.

## 15. Validering och säkerhet

Nätverket får inte publiceras om kritiska fel finns.

Valideringen ska bland annat hitta:

- spår utan giltiga ändpunkter;
- kontrollzoner utan portar;
- portar utan anslutna spår;
- övergångar som refererar till saknade passager;
- ogiltigt definierade stationszoner (t.ex. saknade hörn eller felaktig dimension), men **inte** att en stationszon innehåller flera fysiska rälslinjer;
- **dubbelanvända stationsvisningsnamn** var som helst på servern (efter trimning, skiftlägesoberoende), även vid import eller namnbyte;
- oavsiktligt överlappande kontrollzoner;
- geometri som hoppar orimligt långt;
- spår som byter dimension utan särskild övergång;
- namnlager med ogiltiga intervall;
- dubbletter och trasiga ID-referenser;
- oavsiktlig sammanblandning av spår på olika höjd;
- aktiva resvägar som använder avstängda objekt.

Servern upprätthåller **separata behörigheter för inspektion, namngivning, inspelning och administration/publicering**. Inspelningsbehöriga får publicera *endast ett fullständigt validerat survey-resultat* direkt genom `finish`; övrig skapelse/ändring av fysisk topologi och spårparsbekräftelse kräver administrations-/publiceringsrätt. Namnredaktörer får inte ändra fysiska spår eller rödljusövergångar. Teknisk/adminspecifik information ges bara till behöriga mottagare; normal navigation är öppen för spelare med kompatibel klient.

Destruktiva ändringar kräver förhandsvisning, beroendeanalys och bekräftelse. Systemet ska kunna återgå till föregående fungerande revision.

## 16. Prestanda

Stora nätverk ska hanteras med:

- rumsligt index per dimension och chunk;
- geometrisk förenkling i flera detaljnivåer;
- avståndsbegränsad världsrendering;
- cache av färdiga renderingssegment;
- separata lager som kan stängas av;
- inkrementella nätverksuppdateringar;
- begränsad kandidatmängd vid map matching;
- bakgrundsberäkning där Minecrafts trådsäkerhetsregler tillåter det.

Full detalj behövs nära spelaren och den aktiva resvägen. Avlägsna spår kan visas förenklat.

## 17. Exempelscenario

Följande nät innehåller två parallella spår, flera rödljus och en kort bro:

```text
                         Riksettan
┌──────────────────────────────────────────────────────────────┐
Station A ══ Signal 1 ══ Signal 2 ══ Centralbron ══ Signal 3 ══ Station B
└──────────────────────────────────────────────────────────────┘
```

Den fysiska modellen består av flera sektioner mellan stationerna och signalerna. Varje sektion innehåller ett östgående och ett västgående spår.

Namnet **Riksettan** täcker samtliga relevanta spårintervall och bryts inte av signalerna. Namnet **Centralbron** täcker endast delen mellan brons visuella gränser på båda spåren.

En resa från Station A till Station B använder de östgående passagerna. En resa åt andra hållet använder de separat inspelade västgående passagerna. Båda visar Riksettan, och endast medan vagnen befinner sig på bron visas Centralbron.

Vid Signal 2 kan övergången exempelvis ha instruktionen:

```text
Välj knappen för Martyrön
```

om den planerade utgången kräver ett byggnadsspecifikt knappval. I annat fall beräknas riktningen från spårgeometrin.

## 18. Prioriterad utvecklingsplan

### Fas 1 – Domänmodell och lagring

- Kontrollpunkter, sammansatta 3D-zoner och portar.
- Enkla stationer som **tvåhörnszoner med alla rälslinjer inom zonen inkluderade**; inget plattformssystem eller spårval per station.
- Spårsektioner, fysiska spår, passager och övergångar.
- Transaktionell, revisionshanterad lagring.
- Full nätverksvalidering och JSON-export/import.

### Fas 2 – Synkronisering och grundvisualisering

- **Bygg vidare på `RailwayGpsUi`/`NavigationScreen` från commit `821880e` som gemensam huvudingång (`R`)**. Implementera spelares stationssökning, navigationshantering och personliga inställningar som färdiga UI-flöden, inte kommandoberoende lösningar.
- Server–klient-snapshot med version/hash.
- Inbyggd valfri rendering av zoner, spår och riktningar, **avstängd som standard för vanliga spelare**; HUD visas fortfarande under navigering.
- Visningslägen och behörighetsstyrd administratörsvy.
- Grundläggande nätverksutforskare.

### Fas 3 – Visuell zonredigering

- **Adminverktyg nås via Railway GPS-UI:t**, där hela flödet för tvåhörnszoner, stationer och redlights har förhandsvisning, felhantering och sparande utan kommandon.
- Markering av två hörn i världen.
- Flera boxar per zon.
- Förhandsvisning, mått och överlappsvarningar.
- Stationers **tvåhörnszoner som kan omfatta flera rälslinjer** och rödljusens kontrollzoner.

### Fas 4 – Survey och fysisk nätbyggnad

- **Inspelningsrollen får Starta, Status, Avsluta/publicera och Avbryt i UI:t**. Godkända surveys publiceras direkt/atomiskt när UI:s Avsluta/publicera används; **ingen extra granskning/confirm** införs.
- Serversidig minecart-inspelning.
- Strikt avbrott vid dismount eller annan sessionsförlust.
- Långa inspelningar genom flera kontrollzoner.
- Automatiska delnings-, port- och övergångsförslag.
- **Automatisk, atomisk publicering av helt validerade surveys vid `finish`**, med total avvisning vid fel; separat granskningspublicering krävs inte.

### Fas 5 – Flerspårighet och övergångar

- **Verifiera varje färdriktning separat:** en inspelning bekräftar observerad riktning, medan motsatt riktning kräver egen inspelning eller uttryckligt administratörsgodkännande. Samma geometrilinje återanvänds på verkligt dubbelriktade enkelspår.
- **Geometribaserade förslag på parallella spårpar och obligatoriskt uttryckligt administratörsgodkännande**; inga automatiska parningar.
- Tillåtna/otillåtna rödljusövergångar.
- Kontrollinstruktioner för knappar och specialbyggen.

### Fas 6 – Namnlager

- **Namngivningsrollen gör start/slut-markering, kandidatgranskning och namnsparande via UI:t**; kommandon är valfria alternativ.
- Långa leder genom flera sektioner, exempelvis Riksettan.
- Partiella lokala intervall, exempelvis Centralbron.
- Flera samtidiga namn på samma spår.
- Punkt-till-punkt-namngivning över redan registrerade spår.
- Kandidatval och vid behov mellanpunkter. Om en separat parallell räls **redan är administratörsgodkänd som par**, erbjud ett enkelt **Ja/Nej-val** för att inkludera även dess motsvarande intervall i namnet; **Nej är förvalt**, och inget extra val finns utan par.
- **Enbart enkel punkt-till-punkt-namngivning:** markera start och slut på registrerad räls, ange namn, förhandsvisa och bekräfta; ingen separat namninspelning med minecart.

### Fas 7 – Navigation

- Ruttberäkning över passager och verifierade övergångar.
- Navigering får startas var som helst på en registrerad fysisk räls. Vagnen matchas först mot exakt fysisk
  räls och tillåten färdriktning; en parallell grannräls får inte väljas bara för att den har en giltig rutt.
- Stabil map matching mellan parallella spår.
- **Om vald stations ID har tagits bort ur ny fullständigt verifierad revision, avsluta navigering med tydlig notis, utan ersättningsdestination eller automatiska återförsök.**
- **Automatisk omplanering till kortaste giltiga väg** efter en nätverksrevision/spåravstängning, verifierat avvikande vägval eller byte av destination. **Om ingen giltig väg finns, behåll stationen och försök igen automatiskt först när läget ändras relevant**, utan gamla svänginstruktioner eller tick-spam. När en ny giltig rutt faktiskt ersätter den förra: visa kort **”Rutt omräknad”**, därefter nya instruktioner utan kvittering. Om ingen rutt finns: visa ett tydligt fel utan gamla instruktioner. Hantera osäker rälsposition utan gissning.
- Automatiska och manuella instruktioner.
- Startinstruktionen beräknas från den fysiska inkommande och utgående rälsen **utanför rödljusets zon**. Spelarens kamerariktning får aldrig användas för att avgöra vänster eller höger; **saknas säker rälsmatchning döljs sväng-/knappinstruktionen och Osäker position visas**.
- Lokaliserad HUD med **både avstånd till nästa rödljus och återstående avstånd till stationen längs planerad räls**, **bokstavligt återgivna anpassade instruktioner**, **högst en huvudled plus ett lokalt namn**, och osäkerhetsstatus **utan obekräftade avstånd eller sväng-/knappinstruktioner**.
- **Vid första användningen visas endast HUD**. Spelaren kan separat **slå på/av den planerade rutten som en 3D-linje över verklig yttre räls**, utan JourneyMap. **Lagra visningsvalet lokalt per spelare** mellan resor och omstarter; inga 3D-ritningar finns kvar efter navigeringens slut, men inställningen kvarstår.
- **Sökning och destinationsval endast för stationer i spelarens aktuella dimension**; namn är fortsatt unika **serverövergripande** utan skiftlägesberoende. Samma dimensionsregel gäller meny, kommando och eventuell kartväljare.
- **Destinationsval i förväg tillåts; i stillastående minecart väntar GPS:en på stabil, faktiskt observerad rörelseriktning på säkert registrerad räls innan aktiv rutt väljs**. Under en aktiv resa får spelaren **välja en annan station och omedelbart få ny kortaste giltiga rutt** utan att starta om. All avstigning avslutar navigation, även före ankomst.
- **Automatiskt kortaste giltiga rutt, efter återstående verklig rälsträcka; ingen vägväljarmeny eller prioritet för namngivna leder.**
- **Visa rödljusets vägval när det närmar sig, behåll det inne i zonen tills utfart bekräftats; dölj det alltid vid osäkerhet.**
- **Bekräftad ankomst vid rätt räls/stoppzon från endera separat verifierade riktningen**, även under rörelse; behåll `Framme` tills avstigning.

### Fas 8 – JourneyMap och administrationspolering

- **Färdigställ hela UI:t som primär väg för inspektion, namngivning, survey och adminverktyg**, oavsett om JourneyMap finns installerat. Säkerställ enhetliga serverkontroller för UI och sekundära kommandon.
- Gemensam presentationsmodell för intern rendering och JourneyMap.
- Kartbaserat destinationsval **endast för stationer**.
- Sökning, filtrering och objektinspektion.
- Prestandaoptimering för stora nät.

## 19. Acceptanskriterier

En första komplett version av målarkitekturen ska demonstrera att:

1. Två separata parallella spår mellan samma kontrollpunkter kan spelas in och navigeras i rätt riktning.
2. Ett enkelspår kan markeras som verifierat dubbelriktat utan duplicerad geometri.
3. En lång inspelning genom flera rödljus delas i yttre spår och verifierade black-box-övergångar, och **publiceras atomiskt direkt vid giltigt `finish`** utan separat obligatorisk granskning; vägnamnet behöver inte brytas.
4. Riksettan kan omfatta flera sektioner och kontrollpunkter.
5. Centralbron kan täcka endast en del av spåret utan extra grafnod.
6. Samma spår kan samtidigt visa Riksettan och Centralbron.
7. Namnlösa spår kan ingå i resvägar utan att tekniska ID:n visas för spelaren.
8. **Endast stationer** visas som destinationer. Stationen är en **tvåhörnsmarkerad zon där all räls innanför räknas som stationen**, också när flera spår passerar. **Inga plattformar eller val av enskilt stationsspår krävs.** Ankomst bekräftas vid zonen längs verifierad giltig färdväg även om vagnen rullar. **Framme** visas tills spelaren lämnar minecarten; då avslutas navigationen.
9. En omöjlig sväng i en rödljus används aldrig av ruttberäkningen.
10. Automatisk vänster/höger/rakt-instruktion använder verklig in- och utgående geometri.
11. En anpassad instruktion som `Välj knappen för Martyrön` har företräde.
12. Administratören kan se zoner, spår, riktningar, övergångar, namn och fel visuellt.
13. Vanlig navigation fungerar utan JourneyMap.
14. JourneyMap kan visa samma nät och aktiva resväg när integrationen är installerad.
15. En avbruten inspelning lämnar ingen delvis aktiv data.
16. En felaktig ändring kan inte ersätta den senaste fungerande revisionen.
17. En namngivningsbehörig redaktör kan markera två punkter, välja rätt kandidatväg och namnge de befintliga spåren utan att åka sträckan.
18. Geometriskt föreslagna spårpar förblir oparade tills en administratör uttryckligen godkänner parningen.
19. Separata serverbehörigheter styr inspektion, namngivning, inspelning och administration/publicering utan att ge onödig topologiåtkomst.
20. Vid **osäker position** visas destinationen men ingen sväng-/knappinstruktion förrän spåret identifierats.
21. En behörig spelare kan **starta survey var som helst på rälsen**, och publicering lyckas endast vid säker nätanslutning och full validering.
22. HUD visar högst **en huvudled och ett lokalt rälssträckenamn**; administratörens anpassade instruktioner visas **ordagrant**, oberoende av språk.
23. **Framme** visas kvar tills spelaren **stiger ur minecarten**, då navigeringen avslutas automatiskt.
24. Spelaren får **välja station i sin aktuella dimension till fots**; ingen aktiv rutt eller körinstruktion startar förrän hen sitter i en minecart som **faktiskt rör sig med säkert identifierad färdriktning på registrerad räls**.
25. **All avstigning** avslutar aktiv navigering omedelbart, även **före** ankomst, och senare ombordstigning återupptar inte resan automatiskt.
26. Rödljusets vägvals-/knappinstruktion börjar visas **under infarten när rödljuset närmar sig**, ligger kvar **inne i zonen** tills faktisk utgång bekräftas, och döljs vid osäker position.
27. GPS:en väljer **utan extra fråga den kortaste verifierade rutten** utifrån kvarvarande fysisk rälslängd och tillåtna riktade övergångar; namngivna leder ger ingen förtur och rödljus ger ingen extrakostnad.
28. **Alla stationsvisningsnamn är unika på hela servern**, oberoende av dimension och skillnad i stora/små bokstäver; försök att skapa, byta namn till eller importera ett duplikat avvisas.
29. **Varje färdriktning på fysisk räls verifieras separat:** en inspelning åt ett håll ger inte automatiskt rätt att rutta åt andra hållet, men motsatt passage kan verifieras genom separat inspelning eller administratörsbeslut utan duplicerad geometri.
30. **Ändrat/avstängt spår eller ny nätverksrevision** leder automatiskt till en ny **kortaste giltig rutt** om en finns. Finns ingen rutt visas ett tydligt fel utan fortsatt körvägledning på den ogiltiga rutten.
31. **Namngivning sker enbart via markerad start och slut på registrerad räls plus ett namn**, med förhandsvisning och bekräftelse; någon separat namninspelning genom minecartkörning krävs inte.
32. En station kan nås från **alla verifierade rälslinjer och färdriktningar** som når stationszonen, utan val av en särskild ankomstriktning eller ett stationsspår. En ovaliderad motsatt färdriktning är fortfarande otillåten.
33. Aktiv, säkert matchad HUD visar **både kvarvarande spåravstånd till nästa rödljus och till destinationsstationen**. Saknas nästa rödljus visas ingen falsk distans; vid osäker position döljs opålitliga avstånd.
34. **Vid första användningen visas enbart HUD**, utan inbyggd 3D-ruttmarkering. Spelaren kan valfritt slå på/av **enbart den planerade rutten** i Minecraft-världen; **valet sparas mellan resor och omstarter** och rutten uppdateras vid omplanering och döljs när navigationen avslutas, även utan JourneyMap.
35. Efter att en spelare valt station i förväg eller klivit in i en **stillastående minecart** väntar GPS:en på **säkert observerad rörelse och riktning** innan någon aktiv rutt eller vägvals-/knappinstruktion visas; färdriktning får inte gissas.
36. Vid namngivning av start-/slutmarkerad räls visas ett enkelt **Ja/Nej-val (förvalt Nej)** för att också namnge det **redan administratörsgodkända separata parade spåret**; annars namnges enbart markerad räls. Ingen automatisk parning eller namnspridning.
37. När en annan **giltig rutt faktiskt ersätter** en pågående rutt visas kort den icke-blockerande texten **”Rutt omräknad”**, följd av nya instruktioner; om ingen väg finns visas i stället ett tydligt fel.
38. En administratör skapar station genom att **markera två hörn för en 3D-zon och ange unikt namn**. **Flera rälslinjer inom zonen är fullt tillåtna och alla ingår automatiskt**, utan rälsval, plattformar eller andra stationsobjekt.
39. Under aktiv resa kan spelaren **byta till en annan station**, och GPS:en ersätter då omedelbart resmålet och räknar om **kortaste giltiga rutt från aktuell säkert matchad position/riktning**, utan avstigning eller manuell återstart.
40. Om ingen giltig väg finns **behålls valt resmål** och felet ”Ingen giltig väg till stationen” visas utan ogiltiga instruktioner. GPS:en försöker **automatiskt igen när relevant spårposition, riktning eller nätverksstatus ändras**, och återgår till vägledning om en giltig rutt åter blir möjlig. Ingen manuell retry-knapp eller ständig tick-omplanering.
41. När servern publicerar en **fullständigt validerad revision där spelarens valda stations-ID har tagits bort**, avslutas aktiv navigering, rutt/3D-markering försvinner och spelaren ser **”Stationen har tagits bort”**. Ingen annan station väljs automatiskt och no-route-återförsök mot borttaget ID sker inte.
42. Vanliga spelare ser och kan via **meny, kommando eller kartväljare bara välja stationer i sin aktuella Minecraft-dimension**, även om andra dimensioners stationer finns och teknisk grafdata kan innehålla dimensioner. Namn är ändå **unika serverövergripande**.
43. **Visa planerad 3D-rutt är Av vid första användningen**, men efter att spelaren ändrat inställningen **behålls samma val mellan resor och efter spelstart**, utan JourneyMap. Rutten i världen försvinner när en resa slutar, men själva inställningen återställs inte.
44. **Huvudgränssnittet från SimpleBlueprints-inspirerade commit `821880e`** nås med `R` och låter vanliga spelare **söka/välja/byta station, se status, avsluta navigation och ändra visningsinställningar** utan ett enda kommando eller tekniskt ID; andra dimensioner eller redlights visas inte som destinationer.
45. Var och en av de **fyra fristående behörigheterna** har **fullständiga in-game-UI-arbetsflöden** för sin roll: inspektion, namngivning av räls mellan två markeringar, fysisk survey med direkt/atomisk `finish`, respektive skapande/redigering av stationer/redlights och administratörsgodkännande av spårpar/revisioner. **Ingen av dessa normala funktioner kräver kommando**; sekundära kommandon och UI använder samma serverbehörigheter, validering och resultat.

## 20. Återstående detaljbeslut

Följande **tekniska detaljer**, som inte får ändra de fastställda produktbesluten, behöver prototypas eller beslutas under implementationen:

1. Exakt tolerans och algoritm för **geometriskt grundade** spårparningsförslag (alltid manuell bekräftelse).
2. Exakt format för geometri och detaljnivåer.
3. Hur portar redigeras när flera spår passerar zonkanten mycket nära varandra.
4. Hur en stabil och deterministisk prioritet väljs **inom** kategorierna huvudled respektive lokalt namn (beslutet om högst en av varje är redan fastställt).
5. Exakt sekundär kommandosyntax och UI-layout/knappgruppering. **Att UI:t är huvudvägen och täcker all vanlig administration och alla roller är redan fastställt**, inte ett öppet produktbeslut.
6. Exakta Fabric- och JourneyMap-API-versioner för målversionen.
7. Om ruttberäkningen primärt görs på servern, klienten eller hybridmässigt.
8. Hur tillfälliga avstängningar och enkelriktning under drift exponeras för administratörer.
9. Vilken databasimplementation och backupstrategi som bäst passar målservern.
10. Hur kandidater mellan två markerade punkter **för namngivning** rangordnas och visuellt väljs när flera likvärdiga spårsträckor finns. Namn kopplas aldrig automatiskt till en otydlig förgrening.
11. Hur de **redan beslutade** separata serverbehörigheterna representeras som permission-noder och tilldelas användare.

## 21. Sammanfattning

Railway GPS ska modellera verklig räls, inte bara namngivna linjer mellan punkter. Kontrollpunkter representeras av tydliga 3D-zoner. Stationer är enkla **tvåhörnszoner där all räls inom gränserna hör till stationen**, utan krav på plattformar eller val av specifikt spår. Rödljus har portar och verifierade övergångar. En spårsektion kan innehålla ett, två eller flera fysiska spår med separata riktningar och geometrier.

Namn är fristående lager. Riksettan kan löpa genom flera rödljus, Centralbron kan omfatta endast den stockmarkerade delen av två parallella spår och namnlösa anslutningar kan fortfarande användas fullt ut. En resa beräknas genom det gemensamma fysiska nätet och duplicerar inte spår för varje destinationskombination.

**Det SimpleBlueprints-inspirerade Railway GPS-UI:t från commit `821880e` är huvudingången för både spelare och administratörer; kommandon är valfria sekundära alternativ.** Klienten ska kunna göra nätet synligt på begäran: zoner, spår, riktningar, övergångar, namn, aktiv resväg och valideringsfel kan inspekteras i världen, styrt av separat inspektionsbehörighet. **Vid första användning ser vanliga spelare endast navigations-HUD; därefter styr spelarens sparade val om även den planerade 3D-rutten syns** utan JourneyMap. HUD visar båda avstånden till nästa rödljus och stationen, och stationer kan nås från båda verifierade riktningar på rätt räls. Spelaren får förvälja **en station i sin aktuella dimension** före ombordstigning, navigeringen startar först med säkert observerad riktning i minecart på registrerad räls, och **kan byta till annan station i samma dimension under aktiv resa utan omstart**. Om den valda stationen **tas bort** stoppas navigationen med tydligt meddelande. Spelarens val att rita sin aktiva 3D-rutt sparas lokalt mellan resor och spelstarter, med Av som förstagångsvärde. Om ingen giltig rutt finns behålls vald station med felmeddelande och automatiskt nytt försök vid relevant förändring; all avstigning avslutar aktiv resa. Kortaste giltiga rutt väljs automatiskt, instruktionerna för ett rödljus syns vid infarten och under passage, och stationsnamn måste vara unika på hela servern. JourneyMap använder samma modell men förblir valfritt. **En fullständigt validerad survey publiceras direkt vid `finish`; inga obligatoriska granskningsutkast krävs.** Oparade parallella spår kan publiceras som separata spår, men spårparning kräver alltid administratörsbekräftelse. Spelarnas destinationer är enbart stationer. Ankomst kräver att minecarten når **den valda stationszonen längs verifierad färdväg**, på vilket som helst av spåren i zonen, men inte stillastående vagn; **Framme visas tills spelaren kliver ur minecarten**, varpå navigeringen avslutas automatiskt. Osäkert spår döljer alla sväng-/knappinstruktioner, survey får starta var som helst på rälsen med säker nätanslutning, och HUD visar högst en huvudled plus ett lokalt namn.

Moddet förblir vägledande och icke-ingripande. Det berättar vad spelaren ska göra men styr aldrig minecart, knappar, redstone eller växlar.
