import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

// ===== CUSTOM EXCEPTION CLASSES =====

/**
 * Exception thrown when an email account is not found
 * WHEN TO THROW:
 * - Sending to a non-existent account
 * - Looking up unknown email address
 */
class AccountNotFoundException extends Exception {
    private String emailAddress;
    
    public AccountNotFoundException(String emailAddress) {
        super("Account not found: " + emailAddress);
        this.emailAddress = emailAddress;
    }
    
    public String getEmailAddress() { return emailAddress; }
}

/**
 * Exception thrown when an email is not found
 * WHEN TO THROW:
 * - Looking up email by ID that doesn't exist
 * - Replying/forwarding non-existent email
 */
class EmailNotFoundException extends Exception {
    private String emailId;
    
    public EmailNotFoundException(String emailId) {
        super("Email not found: " + emailId);
        this.emailId = emailId;
    }
    
    public String getEmailId() { return emailId; }
}

/**
 * Exception thrown when email content is invalid
 * WHEN TO THROW:
 * - Empty subject and body
 * - Body exceeds max size
 * - Invalid email address format
 * - Too many recipients
 */
class InvalidEmailException extends Exception {
    public InvalidEmailException(String message) {
        super("Invalid email: " + message);
    }
}

/**
 * Exception thrown when mailbox is full
 * WHEN TO THROW:
 * - Mailbox storage limit exceeded
 * - Too many emails in a folder
 */
class MailboxFullException extends Exception {
    private String emailAddress;
    
    public MailboxFullException(String emailAddress) {
        super("Mailbox full for: " + emailAddress);
        this.emailAddress = emailAddress;
    }
    
    public String getEmailAddress() { return emailAddress; }
}

// ===== ENUMS =====

enum EmailFolder {
    INBOX,      // Received emails
    SENT,       // Sent emails
    DRAFTS,     // Unsent drafts
    TRASH,      // Deleted emails (soft delete)
    SPAM,       // Spam/junk mail
    STARRED     // Important/starred emails
}

enum EmailPriority {
    LOW,        // Newsletter, promotions
    NORMAL,     // Regular correspondence
    HIGH,       // Important/urgent emails
    URGENT      // Time-critical emails
}

enum EmailStatus {
    DRAFT,      // Not yet sent
    SENT,       // Successfully sent
    DELIVERED,  // Delivered to recipient mailbox
    READ,       // Opened by recipient
    FAILED      // Failed to deliver
}

enum EmailLabel {
    WORK,
    PERSONAL,
    FINANCE,
    SOCIAL,
    PROMOTIONS,
    UPDATES
}

// ===== INTERFACES =====

/**
 * Strategy interface for searching emails
 * Allows different search strategies (by subject, body, sender, date range)
 */
interface EmailSearchStrategy {
    List<Email> search(List<Email> emails, String query);
}

/**
 * Strategy interface for spam filtering
 * Allows pluggable spam detection algorithms
 */
interface SpamFilter {
    boolean isSpam(Email email);
}

// ===== STRATEGY IMPLEMENTATIONS =====

/**
 * Search emails by subject line (case-insensitive contains)
 */
class SubjectSearchStrategy implements EmailSearchStrategy {
    /**
     * IMPLEMENTATION HINTS:
     * 1. Filter emails where subject contains query (case-insensitive)
     * 2. Use stream().filter() with toLowerCase().contains()
     * 3. Return matching emails
     */
    @Override
    public List<Email> search(List<Email> emails, String query) {
        // HINT: return emails.stream()
        //     .filter(e -> e.getSubject().toLowerCase().contains(query.toLowerCase()))
        //     .collect(Collectors.toList());
        return emails.stream().filter(e->e.getSubject().toLowerCase().contains(query.toLowerCase())).collect(Collectors.toList());
    }
}

/**
 * Search emails by body content (case-insensitive contains)
 */
class BodySearchStrategy implements EmailSearchStrategy {
    /**
     * IMPLEMENTATION HINTS:
     * 1. Filter emails where body contains query (case-insensitive)
     * 2. Similar to SubjectSearchStrategy but on body field
     */
    @Override
    public List<Email> search(List<Email> emails, String query) {
        // HINT: return emails.stream()
        //     .filter(e -> e.getBody().toLowerCase().contains(query.toLowerCase()))
        //     .collect(Collectors.toList());
        return emails.stream().filter(e->e.getBody().toLowerCase().contains(query.toLowerCase())).collect(Collectors.toList());
    }
}

/**
 * Search emails by sender address (case-insensitive contains)
 */
class SenderSearchStrategy implements EmailSearchStrategy {
    /**
     * IMPLEMENTATION HINTS:
     * 1. Filter emails where sender (from) matches query
     */
    @Override
    public List<Email> search(List<Email> emails, String query) {
        // HINT: return emails.stream()
        //     .filter(e -> e.getFrom().toLowerCase().contains(query.toLowerCase()))
        //     .collect(Collectors.toList());
        return emails.stream().filter(e->e.getFrom().toLowerCase().contains(query.toLowerCase())).collect(Collectors.toList());
    }
}

/**
 * Simple keyword-based spam filter
 * Checks for known spam keywords in subject and body
 */
class KeywordSpamFilter implements SpamFilter {
    private final Set<String> spamKeywords;
    
    public KeywordSpamFilter() {
        spamKeywords = new HashSet<>(Arrays.asList(
            "lottery", "winner", "free money", "click here", "urgent action",
            "nigerian prince", "congratulations", "million dollars"
        ));
    }
    
    /**
     * IMPLEMENTATION HINTS:
     * 1. Convert subject + body to lowercase
     * 2. Check if any spam keyword is contained in the combined text
     * 3. Return true if spam keyword found
     */
    @Override
    public boolean isSpam(Email email) {
        // HINT: String content = (email.getSubject() + " " + email.getBody()).toLowerCase();
        // HINT: return spamKeywords.stream().anyMatch(content::contains);
        String content = (email.getSubject().toLowerCase()+" "+email.getBody().toLowerCase());
        return spamKeywords.stream().anyMatch(content::contains);
    }
}

// ===== DOMAIN CLASSES =====

/**
 * Represents an email attachment
 */
class Attachment {
    private final String fileName;
    private final String fileType;  // e.g., "pdf", "jpg", "doc"
    private final long sizeInBytes;
    
    public Attachment(String fileName, String fileType, long sizeInBytes) {
        this.fileName = fileName;
        this.fileType = fileType;
        this.sizeInBytes = sizeInBytes;
    }
    
    public String getFileName() { return fileName; }
    public String getFileType() { return fileType; }
    public long getSizeInBytes() { return sizeInBytes; }
    
    @Override
    public String toString() {
        return fileName + "." + fileType + " (" + sizeInBytes / 1024 + "KB)";
    }
}

/**
 * Represents an email message
 */
class Email {
    private final String emailId;
    private final String from;
    private final List<String> to;
    private final List<String> cc;
    private final List<String> bcc;
    private String subject;
    private String body;
    private EmailPriority priority;
    private EmailStatus status;
    private EmailFolder folder;
    private final List<Attachment> attachments;
    private final Set<EmailLabel> labels;
    private boolean starred;
    private boolean read;
    private final LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private String replyToEmailId;  // null if not a reply
    
    public Email(String from, List<String> to, String subject, String body) {
        this.emailId = "EMAIL-" + UUID.randomUUID().toString().substring(0, 8);
        this.from = from;
        this.to = new ArrayList<>(to);
        this.cc = new ArrayList<>();
        this.bcc = new ArrayList<>();
        this.subject = subject;
        this.body = body;
        this.priority = EmailPriority.NORMAL;
        this.status = EmailStatus.DRAFT;
        this.folder = EmailFolder.DRAFTS;
        this.attachments = new ArrayList<>();
        this.labels = new HashSet<>();
        this.starred = false;
        this.read = false;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters
    public String getEmailId() { return emailId; }
    public String getFrom() { return from; }
    public List<String> getTo() { return Collections.unmodifiableList(to); }
    public List<String> getCc() { return Collections.unmodifiableList(cc); }
    public List<String> getBcc() { return Collections.unmodifiableList(bcc); }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public EmailPriority getPriority() { return priority; }
    public EmailStatus getStatus() { return status; }
    public EmailFolder getFolder() { return folder; }
    public List<Attachment> getAttachments() { return Collections.unmodifiableList(attachments); }
    public Set<EmailLabel> getLabels() { return Collections.unmodifiableSet(labels); }
    public boolean isStarred() { return starred; }
    public boolean isRead() { return read; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getSentAt() { return sentAt; }
    public String getReplyToEmailId() { return replyToEmailId; }
    
    // Setters
    public void setSubject(String subject) { this.subject = subject; }
    public void setBody(String body) { this.body = body; }
    public void setPriority(EmailPriority priority) { this.priority = priority; }
    public void setStatus(EmailStatus status) { this.status = status; }
    public void setFolder(EmailFolder folder) { this.folder = folder; }
    public void setStarred(boolean starred) { this.starred = starred; }
    public void setRead(boolean read) { this.read = read; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    public void setReplyToEmailId(String replyToEmailId) { this.replyToEmailId = replyToEmailId; }
    public void addCc(String email) { cc.add(email); }
    public void addBcc(String email) { bcc.add(email); }
    public void addAttachment(Attachment attachment) { attachments.add(attachment); }
    public void addLabel(EmailLabel label) { labels.add(label); }
    public void removeLabel(EmailLabel label) { labels.remove(label); }
    
    @Override
    public String toString() {
        return emailId + " [" + from + " → " + to + "] Subject: " + subject 
            + " (" + status + ", " + folder + ", " + (read ? "read" : "unread") + ")";
    }
}

/**
 * Represents a user's email account / mailbox
 */
class EmailAccount {
    private final String emailAddress;
    private final String displayName;
    private final Map<EmailFolder, List<Email>> folders;
    private final long maxStorageBytes;
    private long usedStorageBytes;
    private final LocalDateTime createdAt;
    
    public EmailAccount(String emailAddress, String displayName, long maxStorageBytes) {
        this.emailAddress = emailAddress;
        this.displayName = displayName;
        this.maxStorageBytes = maxStorageBytes;
        this.usedStorageBytes = 0;
        this.createdAt = LocalDateTime.now();
        this.folders = new HashMap<>();
        for (EmailFolder folder : EmailFolder.values()) {
            folders.put(folder, new ArrayList<>());
        }
    }
    
    public String getEmailAddress() { return emailAddress; }
    public String getDisplayName() { return displayName; }
    public Map<EmailFolder, List<Email>> getFolders() { return folders; }
    public long getMaxStorageBytes() { return maxStorageBytes; }
    public long getUsedStorageBytes() { return usedStorageBytes; }
    public void addUsedStorage(long bytes) { usedStorageBytes += bytes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    
    public List<Email> getEmailsInFolder(EmailFolder folder) {
        return folders.getOrDefault(folder, new ArrayList<>());
    }
    
    public void addEmailToFolder(EmailFolder folder, Email email) {
        folders.get(folder).add(email);
    }
    
    public boolean removeEmailFromFolder(EmailFolder folder, Email email) {
        return folders.get(folder).remove(email);
    }
    
    @Override
    public String toString() {
        return displayName + " <" + emailAddress + ">";
    }
}

// ===== EMAIL SERVICE =====

/**
 * Email Service - Low Level Design (LLD)
 * 
 * PROBLEM STATEMENT:
 * Design an email system (like Gmail/Outlook) that can:
 * 1. Create email accounts
 * 2. Compose and send emails (with To, CC, BCC)
 * 3. Receive emails into inbox
 * 4. Reply and Forward emails
 * 5. Organize emails into folders (Inbox, Sent, Drafts, Trash, Spam)
 * 6. Star/unstar and label emails
 * 7. Search emails (by subject, body, sender)
 * 8. Mark as read/unread
 * 9. Delete emails (move to trash, permanent delete)
 * 10. Spam filtering
 * 11. Support attachments
 * 
 * REQUIREMENTS:
 * - Functional: Send, receive, reply, forward, organize, search, filter spam
 * - Non-Functional: Thread-safe, scalable, fast search
 * 
 * DESIGN PATTERNS USED:
 * - Strategy Pattern: Search strategies, spam filters
 * - Observer Pattern: Could be used for new email notifications
 * - Builder Pattern: Could be used for composing complex emails
 * 
 * INTERVIEW HINTS:
 * - Discuss how real email works (SMTP, IMAP, POP3)
 * - Talk about email threading / conversation grouping
 * - Mention search indexing (Lucene/Elasticsearch)
 * - Consider storage limits and quota management
 * - Discuss spam filtering (Bayesian, ML-based)
 */
class EmailService {
    private final Map<String, EmailAccount> accounts;  // emailAddress → EmailAccount
    private final Map<String, Email> allEmails;         // emailId → Email
    private SpamFilter spamFilter;
    private EmailSearchStrategy searchStrategy;
    private final AtomicInteger totalEmailsSent;
    private final AtomicInteger totalEmailsFailed;
    private static final int MAX_RECIPIENTS = 100;
    private static final long MAX_ATTACHMENT_SIZE = 25 * 1024 * 1024; // 25MB
    private static final int MAX_SUBJECT_LENGTH = 500;
    
    public EmailService() {
        this.accounts = new HashMap<>();
        this.allEmails = new HashMap<>();
        this.spamFilter = new KeywordSpamFilter();
        this.searchStrategy = new SubjectSearchStrategy();
        this.totalEmailsSent = new AtomicInteger(0);
        this.totalEmailsFailed = new AtomicInteger(0);
    }
    
    // ===== ACCOUNT MANAGEMENT =====
    
    /**
     * Create a new email account
     * 
     * IMPLEMENTATION HINTS:
     * 1. Validate email address format (contains @ and .)
     * 2. Check if account already exists → throw InvalidEmailException
     * 3. Create new EmailAccount with default storage (1GB = 1073741824 bytes)
     * 4. Store in accounts map
     * 5. Print confirmation and return account
     * 
     * @param emailAddress Valid email address
     * @param displayName User's display name
     * @return Created EmailAccount
     * @throws InvalidEmailException if address invalid or already exists
     */
    public EmailAccount createAccount(String emailAddress, String displayName) throws InvalidEmailException {
        // HINT: if (!emailAddress.contains("@") || !emailAddress.contains("."))
        //           throw new InvalidEmailException("Invalid email format: " + emailAddress);
        // HINT: if (accounts.containsKey(emailAddress))
        //           throw new InvalidEmailException("Account already exists: " + emailAddress);
        // HINT: EmailAccount account = new EmailAccount(emailAddress, displayName, 1073741824L);
        // HINT: accounts.put(emailAddress, account);
        // HINT: System.out.println("✓ Account created: " + account);
        // HINT: return account;
        if(!emailAddress.contains("@") || !emailAddress.contains(".")) throw new InvalidEmailException("Invalid email format: " + emailAddress);
        if(accounts.containsKey(emailAddress)) throw new InvalidEmailException("Account already exists: " + emailAddress);
        // BUG FIX: was !accounts.containsKey — that's inverted! Throw when key ALREADY exists, not when it doesn't
        EmailAccount account = new EmailAccount(emailAddress, displayName, 1073741824L);
        accounts.put(emailAddress,account);
        System.out.println("✓ Account created: " + account);
        return account;
    }
    
    /**
     * Get an email account by address
     * 
     * @param emailAddress Email address to look up
     * @return EmailAccount
     * @throws AccountNotFoundException if account doesn't exist
     */
    public EmailAccount getAccount(String emailAddress) throws AccountNotFoundException {
        // HINT: EmailAccount account = accounts.get(emailAddress);
        // HINT: if (account == null) throw new AccountNotFoundException(emailAddress);
        // HINT: return account;
        EmailAccount account=accounts.get(emailAddress);
        if(account==null) throw new AccountNotFoundException(emailAddress);
        return account;
    }
    
    // ===== COMPOSE & SEND =====
    
    /**
     * Compose and send an email
     * 
     * IMPLEMENTATION HINTS:
     * 1. Validate sender account exists
     * 2. Validate at least one recipient
     * 3. Validate total recipients (to + cc + bcc) <= MAX_RECIPIENTS
     * 4. Validate subject length <= MAX_SUBJECT_LENGTH
     * 5. Create Email object with from, to, subject, body
     * 6. Add cc and bcc if provided
     * 7. Set priority
     * 8. Check spam filter on email
     * 9. For each recipient (to, cc, bcc):
     *    a. Get recipient account (skip if not found - external email)
     *    b. If spam → deliver to SPAM folder
     *    c. Else → deliver to INBOX folder
     *    d. Set email status to DELIVERED
     * 10. Add email to sender's SENT folder
     * 11. Set status to SENT, sentAt to now()
     * 12. Store in allEmails map
     * 13. Increment totalEmailsSent counter
     * 14. Return the email
     * 
     * INTERVIEW DISCUSSION:
     * - How would you handle external recipients (not in our system)?
     * - How to prevent spoofing (sending as someone else)?
     * - Should sending be synchronous or queued?
     * 
     * @param from Sender email address
     * @param to List of recipient email addresses
     * @param cc List of CC addresses (can be empty)
     * @param bcc List of BCC addresses (can be empty)
     * @param subject Email subject
     * @param body Email body
     * @param priority Email priority
     * @return Sent Email object
     * @throws AccountNotFoundException if sender account not found
     * @throws InvalidEmailException if validation fails
     */
    public synchronized Email sendEmail(String from, List<String> to, List<String> cc, 
            List<String> bcc, String subject, String body, EmailPriority priority) 
            throws AccountNotFoundException, InvalidEmailException {
        // HINT: EmailAccount senderAccount = getAccount(from);
        // HINT: if (to == null || to.isEmpty()) throw new InvalidEmailException("At least one recipient required");
        // HINT: int totalRecipients = to.size() + (cc != null ? cc.size() : 0) + (bcc != null ? bcc.size() : 0);
        // HINT: if (totalRecipients > MAX_RECIPIENTS) throw new InvalidEmailException("Too many recipients");
        // HINT: if (subject != null && subject.length() > MAX_SUBJECT_LENGTH)
        //           throw new InvalidEmailException("Subject too long");
        //
        // HINT: Email email = new Email(from, to, subject, body);
        // HINT: email.setPriority(priority);
        // HINT: if (cc != null) cc.forEach(email::addCc);
        // HINT: if (bcc != null) bcc.forEach(email::addBcc);
        //
        // HINT: boolean isSpam = spamFilter.isSpam(email);
        //
        // HINT: // Deliver to all recipients
        // HINT: Set<String> allRecipients = new LinkedHashSet<>();
        // HINT: allRecipients.addAll(to);
        // HINT: if (cc != null) allRecipients.addAll(cc);
        // HINT: if (bcc != null) allRecipients.addAll(bcc);
        //
        // HINT: for (String recipient : allRecipients) {
        //     EmailAccount recipientAccount = accounts.get(recipient);
        //     if (recipientAccount != null) {
        //         Email delivered = email; // In real system, create a copy per recipient
        //         delivered.setFolder(isSpam ? EmailFolder.SPAM : EmailFolder.INBOX);
        //         delivered.setStatus(EmailStatus.DELIVERED);
        //         recipientAccount.addEmailToFolder(isSpam ? EmailFolder.SPAM : EmailFolder.INBOX, delivered);
        //     }
        // }
        //
        // HINT: email.setStatus(EmailStatus.SENT);
        // HINT: email.setSentAt(LocalDateTime.now());
        // HINT: email.setFolder(EmailFolder.SENT);
        // HINT: senderAccount.addEmailToFolder(EmailFolder.SENT, email);
        // HINT: allEmails.put(email.getEmailId(), email);
        // HINT: totalEmailsSent.incrementAndGet();
        // HINT: System.out.println("  ✉️ Sent: " + email);
        // HINT: return email;
        EmailAccount senderAccount = getAccount(from);
        if(to==null || to.isEmpty()) throw new InvalidEmailException("At least one recipient required");
        int totalReceipients = to.size()+(cc!=null?cc.size():0)+(bcc!=null?bcc.size():0);
        if(totalReceipients>MAX_RECIPIENTS) throw new InvalidEmailException("Too many recipients");

        return null;
    }
    
    /**
     * Convenience method: send email with NORMAL priority, no CC/BCC
     */
    public Email sendEmail(String from, List<String> to, String subject, String body)
            throws AccountNotFoundException, InvalidEmailException {
        return sendEmail(from, to, Collections.emptyList(), Collections.emptyList(), 
                subject, body, EmailPriority.NORMAL);
    }
    
    // ===== SAVE DRAFT =====
    
    /**
     * Save an email as draft
     * 
     * IMPLEMENTATION HINTS:
     * 1. Validate sender account exists
     * 2. Create Email object with DRAFT status
     * 3. Set folder to DRAFTS
     * 4. Add to sender's DRAFTS folder
     * 5. Store in allEmails
     * 6. Return the draft
     * 
     * @param from Sender email address
     * @param to Recipients (can be empty for drafts)
     * @param subject Subject
     * @param body Body
     * @return Draft Email object
     * @throws AccountNotFoundException if sender not found
     */
    public Email saveDraft(String from, List<String> to, String subject, String body) 
            throws AccountNotFoundException {
        // HINT: EmailAccount account = getAccount(from);
        // HINT: Email draft = new Email(from, to != null ? to : new ArrayList<>(), subject, body);
        // HINT: draft.setStatus(EmailStatus.DRAFT);
        // HINT: draft.setFolder(EmailFolder.DRAFTS);
        // HINT: account.addEmailToFolder(EmailFolder.DRAFTS, draft);
        // HINT: allEmails.put(draft.getEmailId(), draft);
        // HINT: System.out.println("  📝 Draft saved: " + draft);
        // HINT: return draft;
        return null;
    }
    
    // ===== REPLY & FORWARD =====
    
    /**
     * Reply to an email
     * 
     * IMPLEMENTATION HINTS:
     * 1. Get original email by ID → throw EmailNotFoundException if not found
     * 2. Validate sender account exists
     * 3. Create new email TO = original.from (reply to sender)
     * 4. Set subject = "Re: " + original subject (if not already starting with "Re: ")
     * 5. Set body = replyBody + "\n\n--- Original Message ---\n" + original.body
     * 6. Set replyToEmailId = original emailId
     * 7. Send the reply email
     * 8. Return the reply
     * 
     * @param emailId ID of email to reply to
     * @param replierAddress Who is replying
     * @param replyBody Reply message body
     * @return Reply Email object
     * @throws EmailNotFoundException if original email not found
     * @throws AccountNotFoundException if replier account not found
     * @throws InvalidEmailException if reply is invalid
     */
    public Email reply(String emailId, String replierAddress, String replyBody) 
            throws EmailNotFoundException, AccountNotFoundException, InvalidEmailException {
        // HINT: Email original = allEmails.get(emailId);
        // HINT: if (original == null) throw new EmailNotFoundException(emailId);
        //
        // HINT: String subject = original.getSubject().startsWith("Re: ") 
        //           ? original.getSubject() : "Re: " + original.getSubject();
        // HINT: String fullBody = replyBody + "\n\n--- Original Message ---\n" + original.getBody();
        //
        // HINT: Email reply = sendEmail(replierAddress, Arrays.asList(original.getFrom()),
        //           subject, fullBody);
        // HINT: reply.setReplyToEmailId(emailId);
        // HINT: System.out.println("  ↩️ Reply sent to: " + original.getFrom());
        // HINT: return reply;
        return null;
    }
    
    /**
     * Forward an email to new recipients
     * 
     * IMPLEMENTATION HINTS:
     * 1. Get original email by ID → throw EmailNotFoundException if not found
     * 2. Validate forwarder account exists
     * 3. Create new email TO = forwardTo list
     * 4. Set subject = "Fwd: " + original subject (if not already starting with "Fwd: ")
     * 5. Set body = forwarderNote + "\n\n--- Forwarded Message ---\n" 
     *    + "From: " + original.from + "\nSubject: " + original.subject + "\n\n" + original.body
     * 6. Copy attachments from original
     * 7. Send the forwarded email
     * 8. Return the forwarded email
     * 
     * @param emailId ID of email to forward
     * @param forwarderAddress Who is forwarding
     * @param forwardTo List of recipients to forward to
     * @param forwarderNote Optional note from forwarder
     * @return Forwarded Email object
     */
    public Email forward(String emailId, String forwarderAddress, List<String> forwardTo, String forwarderNote) 
            throws EmailNotFoundException, AccountNotFoundException, InvalidEmailException {
        // TODO: Implement
        // HINT: Email original = allEmails.get(emailId);
        // HINT: if (original == null) throw new EmailNotFoundException(emailId);
        //
        // HINT: String subject = original.getSubject().startsWith("Fwd: ") 
        //           ? original.getSubject() : "Fwd: " + original.getSubject();
        // HINT: String fullBody = (forwarderNote != null ? forwarderNote + "\n\n" : "")
        //           + "--- Forwarded Message ---\n"
        //           + "From: " + original.getFrom() + "\n"
        //           + "Subject: " + original.getSubject() + "\n\n"
        //           + original.getBody();
        //
        // HINT: Email fwd = sendEmail(forwarderAddress, forwardTo, subject, fullBody);
        // HINT: original.getAttachments().forEach(fwd::addAttachment);
        // HINT: System.out.println("  ➡️ Forwarded to: " + forwardTo);
        // HINT: return fwd;
        return null;
    }
    
    // ===== EMAIL ORGANIZATION =====
    
    /**
     * Move email to a different folder
     * 
     * IMPLEMENTATION HINTS:
     * 1. Get email by ID → throw EmailNotFoundException if not found
     * 2. Get account → throw AccountNotFoundException
     * 3. Remove email from current folder
     * 4. Set email's folder to new folder
     * 5. Add email to new folder
     * 6. Print confirmation
     * 
     * @param emailAddress Account to move email in
     * @param emailId Email to move
     * @param targetFolder Destination folder
     */
    public void moveToFolder(String emailAddress, String emailId, EmailFolder targetFolder) 
            throws AccountNotFoundException, EmailNotFoundException {
        // TODO: Implement
        // HINT: EmailAccount account = getAccount(emailAddress);
        // HINT: Email email = allEmails.get(emailId);
        // HINT: if (email == null) throw new EmailNotFoundException(emailId);
        // HINT: account.removeEmailFromFolder(email.getFolder(), email);
        // HINT: email.setFolder(targetFolder);
        // HINT: account.addEmailToFolder(targetFolder, email);
        // HINT: System.out.println("  📂 Moved " + emailId + " to " + targetFolder);
    }
    
    /**
     * Star/unstar an email
     * 
     * IMPLEMENTATION HINTS:
     * 1. Get email by ID → throw EmailNotFoundException
     * 2. Toggle starred status
     * 3. If starring: also add to STARRED folder
     * 4. If unstarring: remove from STARRED folder
     * 
     * @param emailAddress Account
     * @param emailId Email to star/unstar
     * @param star true to star, false to unstar
     */
    public void starEmail(String emailAddress, String emailId, boolean star) 
            throws AccountNotFoundException, EmailNotFoundException {
        // TODO: Implement
        // HINT: EmailAccount account = getAccount(emailAddress);
        // HINT: Email email = allEmails.get(emailId);
        // HINT: if (email == null) throw new EmailNotFoundException(emailId);
        // HINT: email.setStarred(star);
        // HINT: if (star) account.addEmailToFolder(EmailFolder.STARRED, email);
        // HINT: else account.removeEmailFromFolder(EmailFolder.STARRED, email);
        // HINT: System.out.println("  " + (star ? "⭐" : "☆") + " " + emailId);
    }
    
    /**
     * Add label to an email
     * 
     * @param emailId Email ID
     * @param label Label to add
     */
    public void addLabel(String emailId, EmailLabel label) throws EmailNotFoundException {
        // TODO: Implement
        // HINT: Email email = allEmails.get(emailId);
        // HINT: if (email == null) throw new EmailNotFoundException(emailId);
        // HINT: email.addLabel(label);
        // HINT: System.out.println("  🏷️ Label " + label + " added to " + emailId);
    }
    
    /**
     * Mark email as read/unread
     * 
     * @param emailId Email ID
     * @param read true to mark as read, false for unread
     */
    public void markAsRead(String emailId, boolean read) throws EmailNotFoundException {
        // TODO: Implement
        // HINT: Email email = allEmails.get(emailId);
        // HINT: if (email == null) throw new EmailNotFoundException(emailId);
        // HINT: email.setRead(read);
        // HINT: if (read) email.setStatus(EmailStatus.READ);
        // HINT: System.out.println("  " + (read ? "👁️" : "📩") + " " + emailId + " marked " + (read ? "read" : "unread"));
    }
    
    // ===== DELETE =====
    
    /**
     * Delete email (move to trash)
     * 
     * IMPLEMENTATION HINTS:
     * 1. Get email and account
     * 2. Move email to TRASH folder (soft delete)
     * 3. If already in TRASH → permanently delete (remove from all maps)
     * 
     * @param emailAddress Account
     * @param emailId Email to delete
     */
    public void deleteEmail(String emailAddress, String emailId) 
            throws AccountNotFoundException, EmailNotFoundException {
        // TODO: Implement
        // HINT: EmailAccount account = getAccount(emailAddress);
        // HINT: Email email = allEmails.get(emailId);
        // HINT: if (email == null) throw new EmailNotFoundException(emailId);
        //
        // HINT: if (email.getFolder() == EmailFolder.TRASH) {
        //     // Permanent delete
        //     account.removeEmailFromFolder(EmailFolder.TRASH, email);
        //     allEmails.remove(emailId);
        //     System.out.println("  🗑️ Permanently deleted: " + emailId);
        // } else {
        //     // Move to trash
        //     moveToFolder(emailAddress, emailId, EmailFolder.TRASH);
        //     System.out.println("  🗑️ Moved to trash: " + emailId);
        // }
    }
    
    // ===== SEARCH =====
    
    /**
     * Search emails in an account using current search strategy
     * 
     * IMPLEMENTATION HINTS:
     * 1. Get account
     * 2. Collect ALL emails across all folders into one list
     * 3. Apply searchStrategy.search() with the query
     * 4. Return matching emails
     * 
     * @param emailAddress Account to search in
     * @param query Search query
     * @return List of matching emails
     */
    public List<Email> searchEmails(String emailAddress, String query) throws AccountNotFoundException {
        // HINT: EmailAccount account = getAccount(emailAddress);
        // HINT: List<Email> allAccountEmails = account.getFolders().values().stream()
        //     .flatMap(List::stream)
        //     .collect(Collectors.toList());
        // HINT: return searchStrategy.search(allAccountEmails, query);
        EmailAccount account = getAccount(emailAddress);
        List<Email> allAccouEmails = account.getFolders().values().stream().flatMap(List::stream).collect(Collectors.toList());
        return searchStrategy.search(allAccouEmails, query);
    }
    
    /**
     * Set search strategy
     */
    public void setSearchStrategy(EmailSearchStrategy strategy) {
        this.searchStrategy = strategy;
    }
    
    /**
     * Set spam filter
     */
    public void setSpamFilter(SpamFilter filter) {
        this.spamFilter = filter;
    }
    
    // ===== FOLDER QUERIES =====
    
    /**
     * Get emails in a specific folder
     * 
     * IMPLEMENTATION HINTS:
     * 1. Get account
     * 2. Return emails in specified folder, sorted by date (newest first)
     * 
     * @param emailAddress Account
     * @param folder Folder to query
     * @return List of emails in folder (newest first)
     */
    public List<Email> getFolder(String emailAddress, EmailFolder folder) throws AccountNotFoundException {
        // TODO: Implement
        // HINT: EmailAccount account = getAccount(emailAddress);
        // HINT: List<Email> emails = new ArrayList<>(account.getEmailsInFolder(folder));
        // HINT: emails.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        // HINT: return emails;
        return null;
    }
    
    /**
     * Get unread count for an account
     * 
     * @param emailAddress Account
     * @return Number of unread emails in inbox
     */
    public int getUnreadCount(String emailAddress) throws AccountNotFoundException {
        // TODO: Implement
        // HINT: EmailAccount account = getAccount(emailAddress);
        // HINT: return (int) account.getEmailsInFolder(EmailFolder.INBOX).stream()
        //     .filter(e -> !e.isRead())
        //     .count();
        return 0;
    }
    
    /**
     * Get emails by label
     * 
     * @param emailAddress Account
     * @param label Label to filter by
     * @return List of emails with that label
     */
    public List<Email> getEmailsByLabel(String emailAddress, EmailLabel label) throws AccountNotFoundException {
        // TODO: Implement
        // HINT: EmailAccount account = getAccount(emailAddress);
        // HINT: return account.getFolders().values().stream()
        //     .flatMap(List::stream)
        //     .filter(e -> e.getLabels().contains(label))
        //     .collect(Collectors.toList());
        return null;
    }
    
    // ===== ATTACHMENTS =====
    
    /**
     * Add attachment to an email (draft)
     * 
     * IMPLEMENTATION HINTS:
     * 1. Get email → must be in DRAFT status
     * 2. Validate attachment size <= MAX_ATTACHMENT_SIZE
     * 3. Calculate total attachment size (existing + new) → check limit
     * 4. Add attachment to email
     * 
     * @param emailId Email ID (must be a draft)
     * @param attachment Attachment to add
     * @throws EmailNotFoundException if email not found
     * @throws InvalidEmailException if email not a draft or attachment too large
     */
    public void addAttachment(String emailId, Attachment attachment) 
            throws EmailNotFoundException, InvalidEmailException {
        // TODO: Implement
        // HINT: Email email = allEmails.get(emailId);
        // HINT: if (email == null) throw new EmailNotFoundException(emailId);
        // HINT: if (email.getStatus() != EmailStatus.DRAFT)
        //           throw new InvalidEmailException("Can only add attachments to drafts");
        // HINT: long totalSize = email.getAttachments().stream().mapToLong(Attachment::getSizeInBytes).sum();
        // HINT: if (totalSize + attachment.getSizeInBytes() > MAX_ATTACHMENT_SIZE)
        //           throw new InvalidEmailException("Attachment size exceeds limit");
        // HINT: email.addAttachment(attachment);
        // HINT: System.out.println("  📎 Attached: " + attachment);
    }
    
    // ===== STATUS & METRICS =====
    
    /**
     * Display service-wide statistics
     */
    public void displayStatus() {
        System.out.println("\n--- Email Service Status ---");
        System.out.println("Total accounts: " + accounts.size());
        System.out.println("Total emails: " + allEmails.size());
        System.out.println("Sent: " + totalEmailsSent.get() + ", Failed: " + totalEmailsFailed.get());
        
        for (Map.Entry<String, EmailAccount> entry : accounts.entrySet()) {
            EmailAccount acct = entry.getValue();
            System.out.println("\n  📬 " + acct);
            for (EmailFolder folder : EmailFolder.values()) {
                int count = acct.getEmailsInFolder(folder).size();
                if (count > 0) {
                    System.out.println("    " + folder + ": " + count + " emails");
                }
            }
        }
    }
}

// ===== MAIN TEST CLASS =====

public class EmailSystem {
    public static void main(String[] args) {
        System.out.println("=== Email System LLD Test Cases ===\n");
        
        EmailService service = new EmailService();
        
        // Setup: Create accounts
        System.out.println("=== Setup: Create Accounts ===");
        try {
            service.createAccount("alice@email.com", "Alice Johnson");
            service.createAccount("bob@email.com", "Bob Smith");
            service.createAccount("charlie@email.com", "Charlie Brown");
            System.out.println();
        } catch (Exception e) {
            System.out.println("✗ Setup error: " + e.getMessage());
        }
        
        // Test Case 1: Send a simple email
        System.out.println("=== Test Case 1: Send Simple Email ===");
        String sentEmailId = null;
        try {
            Email email = service.sendEmail("alice@email.com", 
                Arrays.asList("bob@email.com"),
                "Hello Bob!", "How are you doing?");
            sentEmailId = email.getEmailId();
            System.out.println("✓ Email sent: " + email);
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 2: Send email with CC and BCC
        System.out.println("=== Test Case 2: Send with CC/BCC ===");
        try {
            Email email = service.sendEmail("alice@email.com",
                Arrays.asList("bob@email.com"),
                Arrays.asList("charlie@email.com"),    // CC
                Collections.emptyList(),                // BCC
                "Team Meeting", "Meeting at 3pm tomorrow",
                EmailPriority.HIGH);
            System.out.println("✓ Email with CC sent: " + email);
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 3: Reply to an email
        System.out.println("=== Test Case 3: Reply to Email ===");
        try {
            if (sentEmailId != null) {
                Email reply = service.reply(sentEmailId, "bob@email.com", 
                    "I'm doing great, thanks for asking!");
                System.out.println("✓ Reply sent: " + reply);
            }
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 4: Forward an email
        System.out.println("=== Test Case 4: Forward Email ===");
        try {
            if (sentEmailId != null) {
                Email fwd = service.forward(sentEmailId, "bob@email.com",
                    Arrays.asList("charlie@email.com"), "FYI - check this out");
                System.out.println("✓ Forwarded: " + fwd);
            }
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 5: Save Draft
        System.out.println("=== Test Case 5: Save Draft ===");
        String draftId = null;
        try {
            Email draft = service.saveDraft("alice@email.com", 
                Arrays.asList("bob@email.com"),
                "Work in progress", "I need to finish writing this...");
            draftId = draft.getEmailId();
            System.out.println("✓ Draft saved: " + draft);
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 6: Add Attachment to Draft
        System.out.println("=== Test Case 6: Add Attachment ===");
        try {
            if (draftId != null) {
                service.addAttachment(draftId, new Attachment("report", "pdf", 2048000));
                service.addAttachment(draftId, new Attachment("photo", "jpg", 1024000));
                System.out.println("✓ Attachments added to draft");
            }
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 7: Star and Label emails
        System.out.println("=== Test Case 7: Star & Label ===");
        try {
            if (sentEmailId != null) {
                service.starEmail("bob@email.com", sentEmailId, true);
                service.addLabel(sentEmailId, EmailLabel.PERSONAL);
                System.out.println("✓ Email starred and labeled");
            }
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 8: Mark as read
        System.out.println("=== Test Case 8: Mark as Read ===");
        try {
            if (sentEmailId != null) {
                service.markAsRead(sentEmailId, true);
                System.out.println("✓ Email marked as read");
                int unread = service.getUnreadCount("bob@email.com");
                System.out.println("  Bob's unread count: " + unread);
            }
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 9: Search emails
        System.out.println("=== Test Case 9: Search Emails ===");
        try {
            // Search by subject
            service.setSearchStrategy(new SubjectSearchStrategy());
            List<Email> results = service.searchEmails("bob@email.com", "Hello");
            System.out.println("✓ Subject search 'Hello': " + (results != null ? results.size() : 0) + " results");
            if (results != null) results.forEach(e -> System.out.println("  " + e));
            
            // Search by sender
            service.setSearchStrategy(new SenderSearchStrategy());
            results = service.searchEmails("bob@email.com", "alice");
            System.out.println("✓ Sender search 'alice': " + (results != null ? results.size() : 0) + " results");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 10: Get folder contents
        System.out.println("=== Test Case 10: View Folders ===");
        try {
            List<Email> inbox = service.getFolder("bob@email.com", EmailFolder.INBOX);
            List<Email> sent = service.getFolder("alice@email.com", EmailFolder.SENT);
            System.out.println("✓ Bob's Inbox: " + (inbox != null ? inbox.size() : 0) + " emails");
            System.out.println("✓ Alice's Sent: " + (sent != null ? sent.size() : 0) + " emails");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 11: Delete email (move to trash)
        System.out.println("=== Test Case 11: Delete Email ===");
        try {
            if (sentEmailId != null) {
                service.deleteEmail("bob@email.com", sentEmailId);
                System.out.println("✓ Email moved to trash");
                
                // Delete again → permanent delete
                service.deleteEmail("bob@email.com", sentEmailId);
                System.out.println("✓ Email permanently deleted");
            }
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 12: Spam detection
        System.out.println("=== Test Case 12: Spam Detection ===");
        try {
            Email spam = service.sendEmail("charlie@email.com",
                Arrays.asList("bob@email.com"),
                "Congratulations! You are a winner!",
                "You have won the lottery! Click here to claim free money!");
            System.out.println("✓ Spam email sent (should be in spam folder)");
            
            List<Email> spamFolder = service.getFolder("bob@email.com", EmailFolder.SPAM);
            System.out.println("  Bob's Spam folder: " + (spamFolder != null ? spamFolder.size() : 0) + " emails");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 13: Exception - Duplicate account
        System.out.println("=== Test Case 13: Exception - Duplicate Account ===");
        try {
            service.createAccount("alice@email.com", "Alice Duplicate");
            System.out.println("✗ Should have thrown InvalidEmailException");
        } catch (InvalidEmailException e) {
            System.out.println("✓ Caught expected exception: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 14: Exception - Account not found
        System.out.println("=== Test Case 14: Exception - Account Not Found ===");
        try {
            service.sendEmail("unknown@email.com", Arrays.asList("bob@email.com"),
                "Test", "Test body");
            System.out.println("✗ Should have thrown AccountNotFoundException");
        } catch (AccountNotFoundException e) {
            System.out.println("✓ Caught expected exception: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 15: Exception - Email not found
        System.out.println("=== Test Case 15: Exception - Email Not Found ===");
        try {
            service.reply("NONEXISTENT-ID", "bob@email.com", "Reply text");
            System.out.println("✗ Should have thrown EmailNotFoundException");
        } catch (EmailNotFoundException e) {
            System.out.println("✓ Caught expected exception: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 16: Get emails by label
        System.out.println("=== Test Case 16: Get by Label ===");
        try {
            List<Email> personalEmails = service.getEmailsByLabel("bob@email.com", EmailLabel.PERSONAL);
            System.out.println("✓ Personal emails: " + (personalEmails != null ? personalEmails.size() : 0));
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Display overall status
        service.displayStatus();
        
        System.out.println("\n=== All Test Cases Complete! ===");
    }
}

/**
 * INTERVIEW DISCUSSION TOPICS:
 * ============================
 * 
 * 1. DESIGN PATTERNS:
 *    Strategy Pattern:
 *      - EmailSearchStrategy: SubjectSearch, BodySearch, SenderSearch
 *      - SpamFilter: KeywordFilter, BayesianFilter, MLFilter
 *      - Easy to swap algorithms at runtime
 *    
 *    Builder Pattern:
 *      - EmailBuilder for composing complex emails
 *      - new EmailBuilder().from(x).to(y).subject(z).cc(w).attach(a).build()
 *    
 *    Observer Pattern:
 *      - Notify user on new email arrival
 *      - Email rules/filters that trigger on conditions
 * 
 * 2. EMAIL PROTOCOLS:
 *    SMTP (Simple Mail Transfer Protocol):
 *      - Used for sending emails
 *      - Port 25 (plain), 587 (TLS), 465 (SSL)
 *      - MTA (Mail Transfer Agent) handles routing
 *    
 *    IMAP (Internet Message Access Protocol):
 *      - Used for receiving/managing emails
 *      - Keeps emails on server
 *      - Syncs across devices
 *    
 *    POP3 (Post Office Protocol):
 *      - Downloads emails to local device
 *      - Removes from server (usually)
 *      - Single device access
 * 
 * 3. STORAGE & SCALABILITY:
 *    Database Design:
 *      - accounts table (email, display_name, quota)
 *      - emails table (id, from, subject, body, status, folder, created_at)
 *      - recipients table (email_id, address, type: TO/CC/BCC)
 *      - attachments table (email_id, file_name, s3_key, size)
 *      - labels table (email_id, label)
 *    
 *    Blob Storage:
 *      - Email bodies in object store (S3)
 *      - Attachments in S3 with CDN
 *      - Metadata in database
 *    
 *    Sharding:
 *      - Shard by user email hash
 *      - Each shard handles subset of mailboxes
 * 
 * 4. SEARCH:
 *    Basic: SQL LIKE queries (slow at scale)
 *    Better: Full-text search index (Elasticsearch/Lucene)
 *    Best: Inverted index on subject, body, sender, labels
 *    Consider: Real-time indexing on email arrival
 * 
 * 5. SPAM FILTERING:
 *    Rule-Based:
 *      - Keyword matching (used here)
 *      - Blacklist/whitelist
 *      - Header analysis
 *    
 *    Statistical:
 *      - Bayesian classifier
 *      - Trained on user feedback (mark as spam/not spam)
 *    
 *    ML-Based:
 *      - Neural networks
 *      - Content + metadata features
 *      - Continuous learning
 *    
 *    Infrastructure:
 *      - SPF/DKIM/DMARC for sender verification
 *      - Rate limiting
 *      - Reputation scoring
 * 
 * 6. EMAIL THREADING:
 *    - Group emails by In-Reply-To / References headers
 *    - Conversation view (like Gmail)
 *    - Thread ID for grouping related emails
 *    - Subject-based threading as fallback
 * 
 * 7. ATTACHMENTS:
 *    - Store in object storage (S3), not database
 *    - Generate pre-signed URLs for download
 *    - Virus scanning before delivery
 *    - Size limits (typically 25MB)
 *    - Content type validation
 * 
 * 8. PUSH NOTIFICATIONS:
 *    - WebSocket for real-time inbox updates
 *    - Push notifications for mobile
 *    - Long polling as fallback
 *    - Server-Sent Events (SSE)
 * 
 * 9. SECURITY:
 *    - TLS encryption in transit
 *    - End-to-end encryption (PGP/S-MIME)
 *    - Authentication (OAuth2, 2FA)
 *    - Phishing detection
 *    - Link scanning
 *    - Sandboxed attachment preview
 * 
 * 10. ADVANCED FEATURES:
 *     - Email templates
 *     - Scheduled send (send later)
 *     - Undo send (delayed dispatch)
 *     - Auto-reply / out-of-office
 *     - Email rules / filters
 *     - Snooze (hide and resurface later)
 *     - Importance indicators
 *     - Read receipts
 *     - Contact management / address book
 *     - Mailing lists / groups
 * 
 * 11. API DESIGN:
 *     POST   /accounts                    - Create account
 *     GET    /accounts/{email}            - Get account info
 *     POST   /emails                      - Send email
 *     POST   /emails/draft                - Save draft
 *     GET    /emails/{id}                 - Get email by ID
 *     POST   /emails/{id}/reply           - Reply to email
 *     POST   /emails/{id}/forward         - Forward email
 *     PUT    /emails/{id}/folder          - Move to folder
 *     PUT    /emails/{id}/star            - Star/unstar
 *     PUT    /emails/{id}/read            - Mark read/unread
 *     PUT    /emails/{id}/labels          - Add/remove labels
 *     DELETE /emails/{id}                 - Delete (trash/permanent)
 *     POST   /emails/{id}/attachments     - Add attachment
 *     GET    /accounts/{email}/inbox      - Get inbox
 *     GET    /accounts/{email}/sent       - Get sent
 *     GET    /accounts/{email}/folders/{f} - Get folder
 *     GET    /accounts/{email}/search?q=  - Search emails
 *     GET    /accounts/{email}/unread     - Get unread count
 * 
 * 12. SYSTEM DESIGN (HIGH LEVEL):
 *     ```
 *     Client → API Gateway → Email Service → Message Queue
 *                                  ↓              ↓
 *                              Database      SMTP Workers
 *                              (metadata)        ↓
 *                                           Mail Server
 *                                               ↓
 *                                        Recipient MX Server
 *     ```
 *     - API Gateway: Auth, rate limiting, routing
 *     - Email Service: Business logic, validation
 *     - Message Queue: Async email sending (SQS/Kafka)
 *     - SMTP Workers: Actual email delivery
 *     - Database: PostgreSQL for metadata, S3 for content
 *     - Search: Elasticsearch for full-text search
 *     - Cache: Redis for inbox counts, recent emails
 */
