# Durum Makineleri

Her durum bir enum, her geçiş tek bir yerden (`canTransitionTo`) kontrol edilir. Geçersiz geçiş exception'dır;
sessizce yutulmaz. Son durumlar (terminal) bir daha değişmez.

## KYC profili

```mermaid
stateDiagram-v2
    [*] --> LEVEL_0
    LEVEL_0 --> PENDING_L1: kimlik bilgisi girildi
    PENDING_L1 --> LEVEL_1: NVİ + tarama temiz
    PENDING_L1 --> MANUAL_REVIEW: isim benzerliği
    PENDING_L1 --> REJECTED: NVİ uyuşmazlığı / kesin eşleşme
    MANUAL_REVIEW --> LEVEL_1: onay
    MANUAL_REVIEW --> REJECTED: red
    LEVEL_1 --> PENDING_L2: uzaktan kimlik tespiti başladı
    PENDING_L2 --> LEVEL_2: NFC + canlılık başarılı
    PENDING_L2 --> LEVEL_1: başarısız (tekrar denenebilir)
    LEVEL_1 --> SUSPENDED: yaptırım listesi güncellemesinde eşleşme
    LEVEL_2 --> SUSPENDED
    SUSPENDED --> LEVEL_1: inceleme sonrası
    REJECTED --> [*]
```

`REJECTED` müşteri için yeni bir başvuru belirli bir süre engellenir; yanlış veri girişiyle deneme yanılma yapılmasın.

## Cüzdan hesabı

```mermaid
stateDiagram-v2
    [*] --> ACTIVE
    ACTIVE --> FROZEN: şüpheli işlem / KYC askıya alındı / müşteri talebi
    FROZEN --> ACTIVE: inceleme sonrası
    ACTIVE --> CLOSED: müşteri kapattı (bakiye 0 olmalı)
    CLOSED --> [*]
```

`FROZEN` hesaba para girişi devam eder, çıkışı durur. Gecikmedeki taksit tahsilatı dondurulmuş hesaptan da yapılır.

## Ödeme niyeti (payment intent)

```mermaid
stateDiagram-v2
    [*] --> REQUIRES_METHOD
    REQUIRES_METHOD --> PROCESSING: yöntem seçildi
    PROCESSING --> REQUIRES_ACTION: 3DS gerekli
    REQUIRES_ACTION --> PROCESSING: 3DS sonucu geldi
    PROCESSING --> AUTHORIZED: capture=MANUAL, blokaj alındı
    PROCESSING --> SUCCEEDED
    PROCESSING --> REQUIRES_METHOD: başarısız, başka yöntem dene
    PROCESSING --> UNKNOWN: dış sistem timeout
    UNKNOWN --> SUCCEEDED: sorgu sonucu
    UNKNOWN --> REQUIRES_METHOD: sorgu: gerçekleşmemiş
    AUTHORIZED --> SUCCEEDED: capture
    AUTHORIZED --> CANCELED: void
    REQUIRES_METHOD --> CANCELED: süre doldu / sipariş iptal
    SUCCEEDED --> [*]
    CANCELED --> [*]
```

İade, intent'in durumunu değiştirmez; ayrı bir `refund` kaydıdır (kısmi iade birden fazla olabilir).

## Giden transfer (IBAN)

```mermaid
stateDiagram-v2
    [*] --> PENDING
    PENDING --> REJECTED: limit / risk / imza
    PENDING --> SENT: blokaj alındı, FAST'e gönderildi
    SENT --> COMPLETED: FAST onayı
    SENT --> FAILED: alıcı reddetti (blokaj serbest)
    SENT --> UNKNOWN: timeout
    UNKNOWN --> COMPLETED
    UNKNOWN --> FAILED
    COMPLETED --> RETURNED: alıcı banka iade etti
    REJECTED --> [*]
    FAILED --> [*]
```

## Gelen transfer

```mermaid
stateDiagram-v2
    [*] --> RECEIVED
    RECEIVED --> CREDITED: cüzdan bulundu, bakiye arttı
    RECEIVED --> RETURNED: cüzdan yok / kapalı / limit aşımı
    CREDITED --> [*]
    RETURNED --> [*]
```

## Kredi teklifi ve başvuru

```mermaid
stateDiagram-v2
    state Teklif {
        [*] --> OFFERED
        OFFERED --> ACCEPTED: müşteri kabul etti
        OFFERED --> EXPIRED: 7 gün doldu
        OFFERED --> WITHDRAWN: yeni değerlendirme eskisini geçersiz kıldı
    }
    state Başvuru {
        [*] --> DRAFT
        DRAFT --> AWAITING_SIGNATURE: ön bilgilendirme gösterildi
        AWAITING_SIGNATURE --> SUBMITTED: sözleşme OTP ile onaylandı
        SUBMITTED --> APPROVED: banka onayı
        SUBMITTED --> DECLINED: banka reddi
        SUBMITTED --> UNKNOWN: banka timeout
        UNKNOWN --> APPROVED
        UNKNOWN --> DECLINED
        DRAFT --> ABANDONED: müşteri vazgeçti
        AWAITING_SIGNATURE --> ABANDONED
    }
```

## Kredi limiti (credit line)

```mermaid
stateDiagram-v2
    [*] --> ACTIVE: banka onayı
    ACTIVE --> SUSPENDED: gecikme başladı (yeni kullanım yok)
    SUSPENDED --> ACTIVE: gecikme kapandı
    ACTIVE --> WITHDRAWN: cayma hakkı (14 gün, kullanım yoksa ya da iade edildiyse)
    SUSPENDED --> ACCELERATED: muacceliyet
    ACCELERATED --> DEFAULTED: 90+ gün, takibe devredildi
    ACTIVE --> CLOSED: müşteri kapattı, borç yok
    ACCELERATED --> CLOSED: tamamı ödendi
    WITHDRAWN --> [*]
    CLOSED --> [*]
    DEFAULTED --> [*]
```

## Kullanım (drawdown)

```mermaid
stateDiagram-v2
    [*] --> REQUESTED
    REQUESTED --> ACTIVE: banka kullandırdı, plan oluştu
    REQUESTED --> REJECTED: limit / banka reddi
    REQUESTED --> UNKNOWN: banka timeout
    UNKNOWN --> ACTIVE
    UNKNOWN --> REJECTED
    ACTIVE --> OVERDUE: bir taksit gecikti
    OVERDUE --> ACTIVE: gecikme ödendi
    ACTIVE --> PAID_OFF: son taksit / erken kapama
    OVERDUE --> PAID_OFF
    REJECTED --> [*]
    PAID_OFF --> [*]
```

`UNKNOWN` kullanım limitten **düşülmüş** sayılır. Belirsizken limiti iade etmek, müşterinin aynı limiti iki kez
kullanmasına yol açabilir.

## Taksit

```mermaid
stateDiagram-v2
    [*] --> SCHEDULED
    SCHEDULED --> DUE: vade günü
    DUE --> PAID: tam tahsilat
    DUE --> PARTIALLY_PAID: kısmi ödeme
    DUE --> OVERDUE: gün bitti
    PARTIALLY_PAID --> OVERDUE
    PARTIALLY_PAID --> PAID
    OVERDUE --> PAID
    SCHEDULED --> PAID: erken ödeme
    PAID --> [*]
```
