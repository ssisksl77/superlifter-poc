## 설명

clojure용 graphql dataloader라이브러리 superlifter를 사용하기 위한 데모.


https://github.com/hlship/boardgamegeek-graphql-proxy 소스코드를 참조.
다른점은 아래와 같다.

- 기존 코드의 leiningen 빌드에 문제가 있어서 `deps.edn` 으로 변경
- 기존 스키마 제거
- superlifter 추가
- 테스트용 comment 추가

pedestal을 사용하고 있으며, 기술스택이 현재 사용하는 것과 다르지만, 문제가 없다고 생각하여 이렇게 진행.

## 사용법

repl을 켜고 `bgg-graphql-proxy.main` 의 `comment` 에 위치한 소스코드로 수행.

쿼리는 `comment`에 설명되어 있지만 아래처럼 부르면 됨. 
스키마는 superlifter 공식문서의 스키마를 따름.

``` sh
curl -XPOST -H "Content-Type:application/graphql" localhost:8888/graphql -d '{pets {id details {name}}}'
```

## License

Copyright © 2017 Howard M. Lewis Ship

follow https://github.com/hlship/boardgamegeek-graphql-proxy
