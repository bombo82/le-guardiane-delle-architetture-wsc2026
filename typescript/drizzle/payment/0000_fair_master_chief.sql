CREATE TABLE `payment_transaction` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`payment_id` text NOT NULL,
	`transaction_id` text NOT NULL,
	`provider` text NOT NULL,
	`provider_reference` text,
	`amount` real NOT NULL,
	`started_at` text NOT NULL,
	`provider_completed_at` text,
	`status` text NOT NULL
);
--> statement-breakpoint
CREATE INDEX `idx_payment_transaction_payment_id` ON `payment_transaction` (`payment_id`);--> statement-breakpoint
CREATE TABLE `payment` (
	`id` text PRIMARY KEY NOT NULL,
	`client_reference` text NOT NULL,
	`amount` real NOT NULL,
	`status` text NOT NULL,
	`provider` text,
	`provider_reference` text,
	`requested_at` text NOT NULL
);
