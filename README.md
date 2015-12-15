# Running dev

```sh
lein do clean, run
lein figwheel
```

# Running prod

```sh
lein do clean, uberjar
docker build -t lonely_twitch .
docker run -p 3000:3000 lonely twitch
```
