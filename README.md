# activemix
Governance Tools for Messaging Systems based on Apache ActiveMQ 5.x

## Security

ActiveMQ security tests use the common [naming convention](https://en.wikipedia.org/wiki/Alice_and_Bob) for security personas.

NOTE: 

### Using JWT

Generate a SSH KeyPair as below. One could use a passphrase to protect the private key, but the `TokenLoginModule` only supports unprotected private keys (empty passphrase) for now. Tests that use password protected keys should use the *Carol* user that, in our convention, always uses protected keys and the password is '*secret*'.


```
$ ssh-keygen -t rsa -b 4096 -C "carol@example.com"
Generating public/private rsa key pair.
Enter file in which to save the key (/Users/hadrian/.ssh/id_rsa): ./id_rsa-carol
Enter passphrase (empty for no passphrase): 
Enter same passphrase again: 
Your identification has been saved in ./id_rsa-carol.
Your public key has been saved in ./id_rsa-carol.pub.
The key fingerprint is:
SHA256:jnbnEbeCyhrf1PYTpu8VAfCviEcD+T8a7sI36zpfDzQ carol@example.com
The key's randomart image is:
+---[RSA 4096]----+
|          ...    |
|         . . .   |
|        o   . .  |
|         o   . . |
|        S = E o  |
|       o = Bo+ . |
|    . o.* XoB..  |
|     = =+*+B.=   |
|    ..+ oOO=+..  |
+----[SHA256]-----+
```

### Generating Tokens

TBD
