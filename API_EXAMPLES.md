# API Usage Examples

Comprehensive examples for using the Email Reader Agent API in various programming languages and tools.

## Table of Contents

- [cURL Examples](#curl-examples)
- [HTTPie Examples](#httpie-examples)
- [JavaScript/Node.js](#javascriptnodejs)
- [Python](#python)
- [Java](#java)
- [Go](#go)
- [PowerShell](#powershell)
- [Postman](#postman)

---

## cURL Examples

### Basic Email Count

```bash
curl -X GET "http://localhost:8080/api/v1/emails/count?senderEmail=superman@example.com"
```

### Pretty Print JSON Response

```bash
curl -X GET "http://localhost:8080/api/v1/emails/count?senderEmail=superman@example.com" | jq '.'
```

### Save Response to File

```bash
curl -X GET "http://localhost:8080/api/v1/emails/count?senderEmail=superman@example.com" \
  -o response.json
```

### With Verbose Output

```bash
curl -v -X GET "http://localhost:8080/api/v1/emails/count?senderEmail=superman@example.com"
```

### Health Check

```bash
curl http://localhost:8080/api/v1/emails/health
```

---

## HTTPie Examples

### Basic Request

```bash
http GET "http://localhost:8080/api/v1/emails/count" senderEmail=="superman@example.com"
```

### With Custom Headers

```bash
http GET "http://localhost:8080/api/v1/emails/count" \
  senderEmail=="superman@example.com" \
  User-Agent:"EmailReader-Client/1.0"
```

---

## JavaScript/Node.js

### Using Fetch API (Browser/Node 18+)

```javascript
// Basic usage
async function getEmailCount(senderEmail) {
  try {
    const response = await fetch(
      `http://localhost:8080/api/v1/emails/count?senderEmail=${encodeURIComponent(senderEmail)}`
    );
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    
    const data = await response.json();
    console.log(`Emails from ${data.senderEmail}: ${data.emailCount}`);
    console.log(`Cached: ${data.cachedResult}`);
    return data;
  } catch (error) {
    console.error('Error fetching email count:', error);
    throw error;
  }
}

// Usage
getEmailCount('superman@example.com')
  .then(data => console.log('Success:', data))
  .catch(error => console.error('Error:', error));
```

### Using Axios

```javascript
const axios = require('axios');

async function getEmailCount(senderEmail) {
  try {
    const response = await axios.get('http://localhost:8080/api/v1/emails/count', {
      params: { senderEmail }
    });
    
    console.log(`Email count: ${response.data.emailCount}`);
    return response.data;
  } catch (error) {
    if (error.response) {
      console.error('Error response:', error.response.data);
    } else {
      console.error('Error:', error.message);
    }
    throw error;
  }
}

// Usage
getEmailCount('superman@example.com');
```

### React Component Example

```jsx
import React, { useState } from 'react';

function EmailCounter() {
  const [email, setEmail] = useState('');
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const response = await fetch(
        `http://localhost:8080/api/v1/emails/count?senderEmail=${encodeURIComponent(email)}`
      );
      
      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message);
      }
      
      const data = await response.json();
      setResult(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <form onSubmit={handleSubmit}>
        <input
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          placeholder="Enter sender email"
          required
        />
        <button type="submit" disabled={loading}>
          {loading ? 'Counting...' : 'Count Emails'}
        </button>
      </form>
      
      {error && <p style={{ color: 'red' }}>{error}</p>}
      
      {result && (
        <div>
          <h3>Results:</h3>
          <p>Sender: {result.senderEmail}</p>
          <p>Email Count: {result.emailCount}</p>
          <p>Cached: {result.cachedResult ? 'Yes' : 'No'}</p>
        </div>
      )}
    </div>
  );
}

export default EmailCounter;
```

---

## Python

### Using Requests Library

```python
import requests

def get_email_count(sender_email):
    """
    Get email count from a specific sender.
    
    Args:
        sender_email (str): Email address of the sender
        
    Returns:
        dict: Response data containing email count
    """
    url = 'http://localhost:8080/api/v1/emails/count'
    params = {'senderEmail': sender_email}
    
    try:
        response = requests.get(url, params=params)
        response.raise_for_status()  # Raise exception for bad status codes
        
        data = response.json()
        print(f"Emails from {data['senderEmail']}: {data['emailCount']}")
        print(f"Cached: {data['cachedResult']}")
        
        return data
    except requests.exceptions.HTTPError as e:
        print(f"HTTP Error: {e}")
        if e.response.status_code == 400:
            error_data = e.response.json()
            print(f"Error message: {error_data['message']}")
    except requests.exceptions.RequestException as e:
        print(f"Error: {e}")
        return None

# Usage
if __name__ == '__main__':
    result = get_email_count('superman@example.com')
    if result:
        print(f"Total emails: {result['emailCount']}")
```

### Using urllib (Standard Library)

```python
import urllib.request
import urllib.parse
import json

def get_email_count(sender_email):
    base_url = 'http://localhost:8080/api/v1/emails/count'
    params = urllib.parse.urlencode({'senderEmail': sender_email})
    url = f'{base_url}?{params}'
    
    try:
        with urllib.request.urlopen(url) as response:
            data = json.loads(response.read().decode())
            return data
    except urllib.error.HTTPError as e:
        print(f"HTTP Error {e.code}: {e.reason}")
        error_data = json.loads(e.read().decode())
        print(f"Error message: {error_data['message']}")
        return None

# Usage
result = get_email_count('superman@example.com')
if result:
    print(f"Email count: {result['emailCount']}")
```

### Async with aiohttp

```python
import aiohttp
import asyncio

async def get_email_count_async(sender_email):
    url = 'http://localhost:8080/api/v1/emails/count'
    params = {'senderEmail': sender_email}
    
    async with aiohttp.ClientSession() as session:
        try:
            async with session.get(url, params=params) as response:
                response.raise_for_status()
                data = await response.json()
                return data
        except aiohttp.ClientError as e:
            print(f"Error: {e}")
            return None

# Usage
async def main():
    result = await get_email_count_async('superman@example.com')
    if result:
        print(f"Email count: {result['emailCount']}")

asyncio.run(main())
```

---

## Java

### Using HttpClient (Java 11+)

```java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EmailReaderClient {
    
    private static final String BASE_URL = "http://localhost:8080/api/v1/emails";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public EmailReaderClient() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }
    
    public EmailCountResponse getEmailCount(String senderEmail) throws Exception {
        String url = BASE_URL + "/count?senderEmail=" + 
                     java.net.URLEncoder.encode(senderEmail, "UTF-8");
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(
            request, 
            HttpResponse.BodyHandlers.ofString()
        );
        
        if (response.statusCode() == 200) {
            return objectMapper.readValue(
                response.body(), 
                EmailCountResponse.class
            );
        } else {
            throw new RuntimeException("HTTP Error: " + response.statusCode());
        }
    }
    
    public static void main(String[] args) {
        try {
            EmailReaderClient client = new EmailReaderClient();
            EmailCountResponse result = client.getEmailCount("superman@example.com");
            
            System.out.println("Email count: " + result.getEmailCount());
            System.out.println("Cached: " + result.isCachedResult());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

// Response DTO
class EmailCountResponse {
    private String senderEmail;
    private long emailCount;
    private boolean cachedResult;
    private String timestamp;
    
    // Getters and setters...
}
```

### Using RestTemplate (Spring)

```java
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

public class EmailReaderService {
    
    private final RestTemplate restTemplate;
    private static final String BASE_URL = "http://localhost:8080/api/v1/emails";
    
    public EmailReaderService() {
        this.restTemplate = new RestTemplate();
    }
    
    public EmailCountResponse getEmailCount(String senderEmail) {
        String url = UriComponentsBuilder.fromHttpUrl(BASE_URL + "/count")
                .queryParam("senderEmail", senderEmail)
                .toUriString();
        
        return restTemplate.getForObject(url, EmailCountResponse.class);
    }
}
```

---

## Go

### Using net/http

```go
package main

import (
    "encoding/json"
    "fmt"
    "io"
    "net/http"
    "net/url"
)

type EmailCountResponse struct {
    SenderEmail  string `json:"senderEmail"`
    EmailCount   int64  `json:"emailCount"`
    CachedResult bool   `json:"cachedResult"`
    Timestamp    string `json:"timestamp"`
}

func getEmailCount(senderEmail string) (*EmailCountResponse, error) {
    baseURL := "http://localhost:8080/api/v1/emails/count"
    
    // Build URL with query parameters
    u, err := url.Parse(baseURL)
    if err != nil {
        return nil, err
    }
    
    q := u.Query()
    q.Set("senderEmail", senderEmail)
    u.RawQuery = q.Encode()
    
    // Make request
    resp, err := http.Get(u.String())
    if err != nil {
        return nil, err
    }
    defer resp.Body.Close()
    
    // Check status code
    if resp.StatusCode != http.StatusOK {
        body, _ := io.ReadAll(resp.Body)
        return nil, fmt.Errorf("HTTP %d: %s", resp.StatusCode, body)
    }
    
    // Parse response
    var result EmailCountResponse
    if err := json.NewDecoder(resp.Body).Decode(&result); err != nil {
        return nil, err
    }
    
    return &result, nil
}

func main() {
    result, err := getEmailCount("superman@example.com")
    if err != nil {
        fmt.Printf("Error: %v\n", err)
        return
    }
    
    fmt.Printf("Email count: %d\n", result.EmailCount)
    fmt.Printf("Cached: %v\n", result.CachedResult)
}
```

---

## PowerShell

### Basic Request

```powershell
# Simple request
$response = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/emails/count?senderEmail=superman@example.com" -Method Get

Write-Host "Email count: $($response.emailCount)"
Write-Host "Cached: $($response.cachedResult)"
```

### With Error Handling

```powershell
function Get-EmailCount {
    param(
        [Parameter(Mandatory=$true)]
        [string]$SenderEmail
    )
    
    $baseUrl = "http://localhost:8080/api/v1/emails/count"
    $uri = "$baseUrl?senderEmail=$([System.Uri]::EscapeDataString($SenderEmail))"
    
    try {
        $response = Invoke-RestMethod -Uri $uri -Method Get -ErrorAction Stop
        
        Write-Host "Sender: $($response.senderEmail)"
        Write-Host "Email Count: $($response.emailCount)"
        Write-Host "Cached Result: $($response.cachedResult)"
        Write-Host "Timestamp: $($response.timestamp)"
        
        return $response
    }
    catch {
        Write-Error "Failed to get email count: $_"
        if ($_.Exception.Response) {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $errorContent = $reader.ReadToEnd()
            Write-Error "Error details: $errorContent"
        }
    }
}

# Usage
Get-EmailCount -SenderEmail "superman@example.com"
```

---

## Postman

### Create a Collection

1. **Create New Request**
   - Method: `GET`
   - URL: `http://localhost:8080/api/v1/emails/count`

2. **Add Query Parameters**
   - Key: `senderEmail`
   - Value: `superman@example.com`

3. **Tests Tab** (Add automatic testing):

```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response has email count", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property('emailCount');
    pm.expect(jsonData.emailCount).to.be.a('number');
});

pm.test("Response has sender email", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.senderEmail).to.eql('superman@example.com');
});
```

---

## Error Handling Examples

### JavaScript - Handle All Error Types

```javascript
async function getEmailCountWithErrorHandling(senderEmail) {
  try {
    const response = await fetch(
      `http://localhost:8080/api/v1/emails/count?senderEmail=${encodeURIComponent(senderEmail)}`
    );
    
    const data = await response.json();
    
    switch (response.status) {
      case 200:
        return data;
      case 400:
        throw new Error(`Invalid email: ${data.message}`);
      case 401:
        throw new Error(`Authentication failed: ${data.message}`);
      case 429:
        throw new Error(`Rate limit exceeded: ${data.message}`);
      case 500:
        throw new Error(`Server error: ${data.message}`);
      default:
        throw new Error(`Unexpected error: ${response.status}`);
    }
  } catch (error) {
    console.error('Error:', error.message);
    throw error;
  }
}
```

---

## Batch Processing Example (Python)

```python
import requests
import time

def batch_count_emails(email_list):
    """Count emails for multiple senders."""
    results = []
    
    for email in email_list:
        try:
            result = get_email_count(email)
            results.append(result)
            time.sleep(0.1)  # Rate limiting
        except Exception as e:
            print(f"Failed for {email}: {e}")
            results.append(None)
    
    return results

# Usage
emails = ['user1@example.com', 'user2@example.com', 'user3@example.com']
results = batch_count_emails(emails)

for result in results:
    if result:
        print(f"{result['senderEmail']}: {result['emailCount']} emails")
```

---

For more examples and latest updates, visit the [Swagger UI](http://localhost:8080/swagger-ui.html).
