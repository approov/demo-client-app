//
//  ViewController.swift
//  shapes-client
//
//  Created by Skip Hovsmith on 1/16/19.
//  Copyright © 2019 Approov. All rights reserved.
//

import UIKit
import Approov

class ViewController: UIViewController {
    
    @IBOutlet weak var statusImageView: UIImageView!
    @IBOutlet weak var statusTextView: UILabel!
    
    private(set) lazy var isSimulator: Bool = {
        var isSim = false
        #if targetEnvironment(simulator)
        isSim = true
        #endif
        return isSim
    }()
    
    var defaultSession = URLSession(configuration: .default)
    
    var approovSession = URLSession(configuration: .approov)
    
    override func viewDidLoad() {
        super.viewDidLoad()
    }
    
    // check unprotected hello endpoint
    
    @IBAction func checkHello() {
        let helloURL = URL(string: "https://demo-server.approovr.io/hello")!
        
        // Display busy screen
        DispatchQueue.main.async {
            self.statusImageView.image = UIImage(named: "approov")
            self.statusTextView.text = "Checking connectivity..."
        }

        let task = defaultSession.dataTask(with: helloURL) { (data, response, error) in
            let message: String
            let image: UIImage?
            
            // analyze response
            if (error == nil) {
                if let httpResponse = response as? HTTPURLResponse {
                    let code = httpResponse.statusCode
                    if code == 200 {
                        // successful http response
                        message = "\(code): OK"
                        image = UIImage(named: "hello")
                    } else {
                        // unexpected http response
                        let reason = HTTPURLResponse.localizedString(forStatusCode: code)
                        message = "\(code): \(reason)"
                        image = UIImage(named: "confused")
                    }
                } else {
                    // not an http response
                    message = "Not an HTTP response"
                    image = UIImage(named: "confused")
                }
            } else {
                // other networking failure
                message = "Unknown networking error"
                image = UIImage(named: "confused")
            }
            
            NSLog("\(helloURL): \(message)")
            
            // Display the image on screen using the main queue
            DispatchQueue.main.async {
                self.statusImageView.image = image
                self.statusTextView.text = message
            }
        }
        
        task.resume()
    }
    
    // check Approov-protected shapes endpoint
    
    @IBAction func checkShape() {
        let shapesURL = URL(string: "https://demo-server.approovr.io/shapes")!
        
        // Display busy screen
        DispatchQueue.main.async {
            self.statusImageView.image = UIImage(named: "approov")
            self.statusTextView.text = "Checking app authenticity..."
        }

        // alert if running in simulator
        if isSimulator {
            showSimAlert()
        }
        
        let task = approovSession.dataTask(with: shapesURL) { (data, response, error) in
            var message: String
            let image: UIImage?
            
            // analyze response
            if (error == nil) {
                if let httpResponse = response as? HTTPURLResponse {
                    let code = httpResponse.statusCode
                    if code == 200 {
                        // successful http response
                        message = "\(code): Approoved!"
                        let text = String(data: data!, encoding: String.Encoding.utf8)!.lowercased()
                        switch text {
                        case "circle":
                            image = UIImage(named: "circle")
                        case "rectangle":
                            image = UIImage(named: "rectangle")
                        case "square":
                            image = UIImage(named: "square")
                        case "triangle":
                            image = UIImage(named: "triangle")
                        default:
                            message = "\(code): Approoved: unknown shape '\(text)'"
                            image = UIImage(named: "confused")
                        }
                    } else {
                        // unexpected http response
                        let reason = HTTPURLResponse.localizedString(forStatusCode: code)
                        message = "\(code): \(reason)"
                        image = UIImage(named: "confused")
                    }
                } else {
                    // not an http response
                    message = "Not an HTTP response"
                    image = UIImage(named: "confused")
                }
            } else {
                // other networking failure
                message = "Unknown networking error"
                image = UIImage(named: "confused")
            }
            
            NSLog("\(shapesURL): \(message)")
            
            // Display the image on screen using the main queue
            DispatchQueue.main.async {
                self.statusImageView.image = image
                self.statusTextView.text = message
            }
        }
        
        task.resume()
        
    }
    
    func showSimAlert() {
        let title = "Warning: Simulator Detected"
        let message = "Approov attestation checking requires a real iOS device. See the info screen for additional information."
        
        let alert = UIAlertController(title: title, message: message, preferredStyle: .alert)
        let action = UIAlertAction(title: "Close", style: .default, handler: nil)
        alert.addAction(action)
        present(alert, animated: true, completion: nil)
    }
}
