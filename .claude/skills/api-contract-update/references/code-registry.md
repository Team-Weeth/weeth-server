# API Code Registry

## Code Format `XDDNN`

| Part | Meaning |
|------|---------|
| X | 1=Success, 2=Domain Error, 3=Infra/Server Error, 4=Client/Validation Error |
| DD | Domain ID (01~99) |
| NN | Sequence within domain (00~99) |

## Domain IDs

| DD | Domain | Success | Domain Error | Infra Error |
|----|--------|---------|--------------|-------------|
| 01 | account | 10100~ | 20100~ | - |
| 02 | attendance | 10200~ | 20200~ | - |
| 03 | session | 10300~ | 20300~ | - |
| 04 | board | 10400~ | 20400~ | - |
| 05 | comment | 10500~ | 20500~ | - |
| 06 | file | 10600~ | 20600~ | 30600~ |
| 07 | penalty | 10700~ | 20700~ | - |
| 08 | schedule | 10800~ | 20800~ | - |
| 09 | user | 10900~ | 20900~ | - |
| 10 | cardinal | 11000~ | 21000~ | - |
| 11 | club | 11100~ | 21100~ | - |
| 12 | dashboard | 11200~ | 21200~ | - |
| 13 | university | 11300~ | - | 31300~ |
| 90 | jwt/auth | - | 29000~ | - |
| 99 | common | - | - | 39900~ |

## Naming And Location

- Success enum: `{Domain}ResponseCode` in `presentation/`.
- Error enum: `{Domain}ErrorCode` in `application/exception/`.
- Irregulars:
  - schedule's error enum is `EventErrorCode`.
  - JWT codes live in `global/auth/jwt/application/exception/JwtErrorCode`.

Before assigning a code, inspect the existing enum and choose the next unused sequence for that domain/category.
