# Interoperable Chain of Custody based on Distributed Ledger Technology in Digital Forensics (CO-COC)

## Running the Model Chain of Custody
---
Flows akan di inisiasi melalui endpoint berikut `POST /flow/{holdingidentityshorthash}` dan agar dapat memeriksa hasiil dari inisiasi suatu flow, maka perlu dipanggil endpoint berikut untuk menunjukkan hasilnya `GET /flow/{holdingidentityshorthash}/{clientrequestid}`
* holdingidentityshorthash: identifier dari anggota yang terdaftar di dalam cluster.
* clientrequestid: identifier unik yang dikembalikkan suatu flow requestBody ketika berhasil diinisiasi.
---
### CASE REPORT
#### UC1: Create Case Report
Berdasarkan aturan otoritas yang telah dibuat, hanya investigator yang boleh menginisiasi perintah `create` pada `CreateCaseReportFlow`. 

Masukkan endpoint berikut `POST /flow/{holdingidentityshorthash}`, masukkan identitas petugas dan request body:
```
{
  "clientRequestId": "createCR-01",
    "flowClassName": "com.r3.developers.chainofcustody.digitalevidence.CreateCaseReport",
    "requestBody": {
        "caseNumber":"content identifier",
        "caseName":"DE-01821398",
        "suspectName":"flashdisk",
        "victimName":"lite",
        "locationCase":"Sundis",
        "dateNtime":"Sundis-01821379823",
        "toolName":"file yang berisi informasi yang diduga berita hoax",
        "toolsDesc":"CC-001",
        "firstResponder":"pak rudi"
        "organisationName":"org1"
        "validationStatus":"VALIDATE"
        "otherMember":"CN=Custodian, OU=CrimeInvestigationTeam, O=Org1, L=Makassar, C=ID"
   }
}
```

Setelah itu, eksekusi endpoint berikut `GET /flow/{holdingidentityshorthash}/{clientrequestid}` dan masukkan identitas petugas serta clientrequestid untuk melihat hasil dari flow yang telah diinisiasi sebelumnya.

// SELESAIKAN DULU LIST UNTUK TIAP PACKAGE

#### UC2: Update Case Report
In order to continue the chat, we would need the chat ID. This step will bring out all the chat entries this entity (Alice) has.
Go to `POST /flow/{holdingidentityshorthash}`, enter the identity short hash(Alice's hash) and request body:
```
{
    "clientRequestId": "list-1",
    "flowClassName": "com.r3.developers.cordapptemplate.utxoexample.workflows.ListChatsFlow",
    "requestBody": {}
}
```
After trigger the list-chats flow, again, we need to hop to `GET /flow/{holdingidentityshorthash}/{clientrequestid}` and check the result. As the screenshot shows, in the response body,
we will see a list of chat entries, but it currently only has one entry. And we can see the id of the chat entry. Let's record that id.


#### Step 3: Continue the chat with `UpdateChatFlow`
In this step, we will continue the chat between Alice and Bob.
Goto `POST /flow/{holdingidentityshorthash}`, enter the identity short hash and request body. Note that here we can have either Alice or Bob's short hash. If you enter Alice's hash,
this message will be recorded as a message from Alice, vice versa. And the id field is the chat entry id we got from the previous step.
```
{
    "clientRequestId": "update-1",
    "flowClassName": "com.r3.developers.cordapptemplate.utxoexample.workflows.UpdateChatFlow",
    "requestBody": {
        "id":" ** fill in id **",
        "message": "How are you today?"
        }
}
```
And as for the result of this flow, go to `GET /flow/{holdingidentityshorthash}/{clientrequestid}` and enter the required fields.

#### Step 4: See the whole chat history of one chat entry
After a few back and forth of the messaging, you can view entire chat history by calling GetChatFlow.

```
{
    "clientRequestId": "get-1",
    "flowClassName": "com.r3.developers.cordapptemplate.utxoexample.workflows.GetChatFlow",
    "requestBody": {
        "id":" ** fill in id **",
        "numberOfRecords":"4"
    }
}
```
And as for the result, you need to go to the Get API again and enter the short hash and client request ID.

Thus, we have concluded a full run through of the chat app. 
