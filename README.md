## 설명

clojure용 graphql dataloader라이브러리 superlifter를 사용하기 위한 데모.

pedestal을 사용하고 있으며, 기술스택이 현재 사용하는 것과 다르지만, 문제가 없다고 생각하여 이렇게 진행.

## 사용법

repl을 켜고 `bgg-graphql-proxy.main` 의 `comment` 에 위치한 소스코드로 수행.

쿼리는 `comment`에 설명되어 있지만 아래처럼 부르면 됨. 
스키마는 superlifter 공식문서의 스키마를 따름.

``` sh
curl -XPOST -H "Content-Type:application/graphql" localhost:8888/graphql -d '{pets {id details {name}}}'
```

혹은 graphiql을 사용하면 된다. `localhost:8888/` 로 접속하면 된다. 

## superlifter 사용/미사용 하면서 비교하기

`main.clj`를 파일 내 주석을 보면

- `without superlifter START|END` 사이의 함수주석을 풀면 `superlifter` 사용하지 않는 리졸버로 대체.
- `with superlifter START|END` 사이의 함수주석을 풀면 `superlifter` 사용하는 리졸버로 대체. 

