# ItemBlocker

**Zyskaj pełną kontrolę nad przedmiotami, których gracze mogą używać na Twoim serwerze**

ItemBlocker to lekki, ale potężny plugin, który pozwala na blokowanie konkretnych przedmiotów przed użyciem, craftowaniem, podnoszeniem i nie tylko. Idealny do kontrolowania zbyt silnych przedmiotów, zapobiegania griefingowi lub tworzenia unikalnych doświadczeń z rozgrywki.

## Funkcje

### Pełna kontrola przedmiotów

Blokuj przedmioty na 7 różnych sposobów - zapobiegaj craftowaniu, podnoszeniu, wyrzucaniu, używaniu, stawianiu bloków, zakładaniu zbroi oraz przenoszeniu przedmiotów w pojemnikach (skrzynkach itp.). Każda akcja może być włączona lub wyłączona niezależnie, co zapewnia maksymalną elastyczność.

### Inteligentny system wiadomości

Gracze otrzymują jasne informacje zwrotne, gdy próbują użyć zablokowanych przedmiotów. Wbudowany system cooldown zapobiega spamowaniu wiadomościami. Wszystkie wiadomości są w pełni konfigurowalne i wspierają kody kolorów.

### Obsługa wielu języków

Natychmiastowa zmiana między polskim a angielskim lub możliwość stworzenia własnego pliku językowego. Idealne dla serwerów międzynarodowych.

### Proste zarządzanie

- Dodawaj przedmioty za pomocą `/ib add <item>` lub `/ib addhand` dla przedmiotu trzymanego w ręce
- Usuwaj je równie łatwo komendą `/ib remove <item>`
- Sprawdzaj status przedmiotu przez `/ib check <item>`
- Pełne wsparcie Tab-complete dla wszystkich komend

### Elastyczne uprawnienia

Daj graczom VIP lub administracji możliwość omijania blokad dzięki uprawnieniu `itemblocker.bypass`. Działa z każdym pluginem na uprawnienia (LuckPerms, PEX itp.).

### Zero wpływu na wydajność

Optymalna obsługa zdarzeń zapewnia płynne działanie serwera nawet przy setkach zablokowanych przedmiotów. Testowane na serwerach z ponad 100 graczami online.

### Konfiguracja na żywo

Aktualizuj konfigurację i przeładuj ją komendą `/ib reload` - bez potrzeby restartowania serwera. Zmieniaj języki, zablokowane przedmioty lub ustawienia "w locie".

## Szybki start

1. Pobierz i umieść plugin w folderze `plugins/`
2. Zrestartuj serwer
3. Użyj komendy `/ib add <item>`, aby zablokować przedmioty
4. Konfiguruj w folderze `plugins/ItemBlocker/`

## Komendy

Wszystkie komendy wspierają podpowiedzi (tab completion):

```
/ib add <item>       - Blokuje przedmiot
/ib remove <item>    - Odblokowuje przedmiot
/ib addhand          - Blokuje przedmiot trzymany w ręce
/ib list             - Pokazuje listę zablokowanych przedmiotów
/ib check <item>     - Sprawdza, czy przedmiot jest zablokowany
/ib reload           - Przeładowuje konfigurację pluginu
/ib help             - Pokazuje pomoc dotyczącą komend
```

**Aliasy:** `/itemblocker`, `/blocker`

## Uprawnienia

```
itemblocker.*        - Pełny dostęp do wszystkich funkcji
itemblocker.add      - Możliwość dodawania przedmiotów do blokady
itemblocker.remove   - Możliwość usuwania przedmiotów z blokady
itemblocker.bypass   - Omijanie wszystkich blokad (VIP/Administracja)
itemblocker.reload   - Możliwość przeładowania konfiguracji
itemblocker.list     - Możliwość podglądu listy zablokowanych przedmiotów
```

## Konfiguracja

**Prosty plik config.yml:**

```yaml
# Język (en/pl)
language: "pl"

# Akcje do zablokowania
block-actions:
  crafting: true
  pickup: true
  drop: true
  use: true
  place: true
  armor: true
  inventory: true

# Ustawienia bypass
bypass-permission:
  enabled: true
```

**Personalizacja wiadomości:**

Wszystkie wiadomości są w pełni konfigurowalne w plikach `languages/messages_en.yml` oraz `languages/messages_pl.yml`.

## Przykłady

**Blokowanie niebezpiecznych przedmiotów:**

```
/ib add TNT
/ib add BEDROCK
/ib add COMMAND_BLOCK
```

**Blokowanie przedmiotu trzymanego w ręce:**

```
/ib addhand
```

**Nadanie omijania blokad dla rangi VIP (LuckPerms):**

```
/lp group vip permission set itemblocker.bypass true
```

## Wymagania

- Java 21+
- Paper lub Spigot 1.21+

## Wsparcie

- **Błędy:** [GitHub Issues](https://github.com/kuqk/ItemBlocker/issues)
- **Źródło:** [GitHub Repository](https://github.com/kuqk/ItemBlocker)

## Dlaczego ItemBlocker?

- ✅ Lekki i szybki (Zero lagów)
- ✅ Łatwy w obsłudze
- ✅ Wysoka konfigurowalność
- ✅ Aktywnie rozwijany
- ✅ Open source (Licencja MIT)
- ✅ Pełna obsługa wielu języków
- ✅ Brak wymaganych dodatkowych bibliotek

Idealny dla serwerów survival, minigier, własnych trybów rozgrywki i wielu innych!
